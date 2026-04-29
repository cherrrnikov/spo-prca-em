import { modal } from '$lib/services/modal.service';
import type { ModeCreationForm, TimeInterval } from '$lib/types';
import { checkIntervalOverlap } from '$lib/utils/interval/conflicts';
import { IntervalValidationService } from '$lib/utils/intervalValidation';
import { TimeUtils } from '$lib/utils/time';
import { get } from 'svelte/store';
import { ScheduleConverterService } from '../../../../features/services/scheduleConverter.service';
import type { createCreators } from '../creators';
import type { createStores } from '../stores';
import type { createValidation } from '../validation';

export function createIntervalHandlers(
    stores: ReturnType<typeof createStores>,
    creators: ReturnType<typeof createCreators>,
    validation: ReturnType<typeof createValidation>
) {
    const {
        intervals,
        createdPrograms,
        editingInterval,
        selectedIntervalId,
        selectedIntervalIds,
        selectedMode,
        isEditing
    } = stores;

    const {
        createUpdatedInterval,
        createProgramModeData
    } = creators;

    const {
        updateAllConflicts,
        syncCurrentProgramWithStore
    } = validation;

    function handleIntervalClick(interval: TimeInterval, isMultiSelect = false) {
        isEditing.set(true);

        const intervalWithDefaults = {
            ...interval,
            msu1Config: interval.msu1Config || ScheduleConverterService.getDefaultMsuConfig(),
            msu2Config: interval.msu2Config || ScheduleConverterService.getDefaultMsuConfig(),
            customerCode: interval.customerCode || 1
        };

        if (isMultiSelect) {
            const currentEditingInterval = get(editingInterval);
            if (currentEditingInterval && currentEditingInterval.mode !== interval.mode) {
                return; // разные режимы — игнорируем
            }

            selectedIntervalIds.update(ids => {
                const next = new Set(ids);
                if (next.has(interval.id)) {
                    next.delete(interval.id);
                } else {
                    next.add(interval.id);
                }
                return next;
            });

            if (!currentEditingInterval) {
                editingInterval.set(intervalWithDefaults);
                selectedIntervalId.set(interval.id);
                selectedMode.set(interval.mode);
                selectedIntervalIds.update(ids => new Set([...ids, interval.id]));
            }
        } else {
            // Обычный клик — сбрасываем мультивыбор
            selectedIntervalIds.set(new Set([interval.id]));
            editingInterval.set(intervalWithDefaults);
            selectedIntervalId.set(interval.id);
            selectedMode.set(interval.mode);
        }
    }

    function handleIntervalDelete(intervalId: string) {
        const idsToDelete = get(selectedIntervalIds);

        const targetIds = idsToDelete.has(intervalId) && idsToDelete.size > 0
            ? idsToDelete
            : new Set([intervalId]);

        intervals.update(current => current.filter(i => !targetIds.has(i.id)));
        createdPrograms.update(current =>
            current.filter(p => !targetIds.has(p.timeInterval.id))
        );

        const currentEditingInterval = get(editingInterval);
        if (currentEditingInterval && targetIds.has(currentEditingInterval.id)) {
            const deletedMode = currentEditingInterval.mode;
            editingInterval.set(null);
            selectedIntervalId.set(null);
            selectedIntervalIds.set(new Set());
            selectedMode.set(deletedMode);
            isEditing.set(false);
        }

        updateAllConflicts();
        syncCurrentProgramWithStore();
    }

    function handleIntervalUpdate(formData: ModeCreationForm) {
        const currentEditingInterval = get(editingInterval);
        if (!currentEditingInterval) return;

        const validation = IntervalValidationService.validateTimeInput(
            formData.startTime,
            formData.duration
        );

        if (!validation.isValid) {
            modal.alert("Ошибка", `${validation.message}`, 'error');
            return;
        }

        const currentIntervals = get(intervals);
        const idsToUpdate = get(selectedIntervalIds);
        const isMulti = idsToUpdate.size > 1;

        if (!isMulti) {
            const sameModeOverlap = checkIntervalOverlap(
                currentIntervals,
                formData.startTime,
                formData.duration,
                currentEditingInterval.mode,
                currentEditingInterval.id
            );

            if (sameModeOverlap.overlaps) {
                modal.alert("Ошибка", `Интервал пересекается с существующим интервалом\n` +
                    `Время конфликта: ${sameModeOverlap.conflictingInterval?.startTime} - ${sameModeOverlap.conflictingInterval?.endTime}`, 'error');
                return;
            }

            const endTime = TimeUtils.calculateEndTimeSeconds(formData.startTime, formData.duration);
            const updatedInterval = createUpdatedInterval(currentEditingInterval, formData, endTime);

            intervals.update(current =>
                current.map(interval =>
                    interval.id === currentEditingInterval.id ? updatedInterval : interval
                )
            );

            createdPrograms.update(current =>
                current.map(program => {
                    if (program.timeInterval.id === currentEditingInterval.id) {
                        const modeData = createProgramModeData(formData, program.tempId);
                        return { ...program, modeData, timeInterval: updatedInterval };
                    }
                    return program;
                })
            );
        } else {
            // Мультиредактирование — время не трогаем
            intervals.update(current =>
                current.map(interval => {
                    if (!idsToUpdate.has(interval.id)) return interval;
                    const duration = interval.dlit ?? TimeUtils.timeToSeconds(interval.endTime) - TimeUtils.timeToSeconds(interval.startTime);
                    const endTime = TimeUtils.calculateEndTimeSeconds(interval.startTime, duration);
                    return createUpdatedInterval(interval, {
                        ...formData,
                        startTime: interval.startTime,
                        duration: duration
                    }, endTime);
                })
            );

            createdPrograms.update(current =>
                current.map(program => {
                    if (!idsToUpdate.has(program.timeInterval.id)) return program;
                    const duration = program.timeInterval.dlit ?? TimeUtils.timeToSeconds(program.timeInterval.endTime) - TimeUtils.timeToSeconds(program.timeInterval.startTime);
                    const modeData = createProgramModeData({
                        ...formData,
                        startTime: program.timeInterval.startTime,
                        duration: duration
                    }, program.tempId);
                    return { ...program, modeData };
                })
            );
        }

        updateAllConflicts();
        editingInterval.set(null);
        selectedIntervalId.set(null);
        selectedIntervalIds.set(new Set());
        isEditing.set(false);
        syncCurrentProgramWithStore();
    }

    return {
        handleIntervalClick,
        handleIntervalDelete,
        handleIntervalUpdate
    };
}
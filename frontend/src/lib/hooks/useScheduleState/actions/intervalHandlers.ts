import { MODE_CODES } from '$lib/constants/schedule';
import { modal } from '$lib/services/modal.service';
import type { ModeCreationForm, TimeInterval } from '$lib/types';
import { checkIntervalOverlap } from '$lib/utils/interval/conflicts';
import { IntervalValidationService } from '$lib/utils/intervalValidation';
import { TimeUtils } from '$lib/utils/time';
import { get } from 'svelte/store';
import { ScheduleConverterService } from '../../../../features/services/data/scheduleConverter.service';
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

    function handleIntervalClick(interval: TimeInterval) {
        isEditing.set(true);

        const intervalWithDefaults = {
            ...interval,
            msu1Config: interval.msu1Config || ScheduleConverterService.getDefaultMsuConfig(),
            msu2Config: interval.msu2Config || ScheduleConverterService.getDefaultMsuConfig(),
            customerCode: interval.customerCode || 1
        };
        
        editingInterval.set(intervalWithDefaults);
        selectedIntervalId.set(interval.id);
        selectedMode.set(interval.mode);
    }

    function handleIntervalDelete(intervalId: string) {

        const currentIntervals = get(intervals);
        const intervalToDelete = currentIntervals.find(i => i.id === intervalId);

        intervals.set(currentIntervals.filter(interval => interval.id !== intervalId));

        const currentPrograms = get(createdPrograms);

        const filteredPrograms = currentPrograms.filter(program => program.timeInterval.id !== intervalId);
        createdPrograms.set(filteredPrograms);
        
        const currentEditingInterval = get(editingInterval);
        if (currentEditingInterval?.id === intervalId) {
            const deletedMode = currentEditingInterval.mode;
            editingInterval.set(null);
            selectedIntervalId.set(null);
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
            modal.alert("Ошибка", `${validation.message}`, 'error')
            return;
        }

        const currentIntervals = get(intervals);
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
                    if (modeData.kodMode === MODE_CODES.TS) {
                    }
                    return { 
                        ...program, 
                        modeData, 
                        timeInterval: updatedInterval 
                    };
                }
                return program;
            })
        );
        
        updateAllConflicts();
        editingInterval.set(null);
        selectedIntervalId.set(null);
        isEditing.set(false);
        syncCurrentProgramWithStore();
    }

    return {
        handleIntervalClick,
        handleIntervalDelete,
        handleIntervalUpdate
    };
}
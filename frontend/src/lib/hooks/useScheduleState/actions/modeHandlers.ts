import type { ModeCreationForm } from '$lib/types';
import { checkIntervalOverlap } from '$lib/utils/interval/conflicts';
import { IntervalValidationService } from '$lib/utils/intervalValidation';
import { TimeUtils } from '$lib/utils/time';
import { get } from 'svelte/store';
import type { createCreators } from '../creators';
import type { createStores } from '../stores';
import type { createValidation } from '../validation';

export function createModeHandlers(
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
        isEditing,
        operatorDataLoaded,
        creationMode
    } = stores;

    const {
        generateTempId,
        createTimeInterval,
        createProgramModeData
    } = creators;

    const {
        checkAndUpdateAllConflictsForNewInterval,
        updateAllConflicts,
        syncCurrentProgramWithStore
    } = validation;

    function handleModeSelect(modeId: number) {
        if (!get(operatorDataLoaded) || get(creationMode) !== 'operator') {
            return;
        }
        selectedMode.set(modeId);
    }

    function handleModeFormSubmit(formData: ModeCreationForm) {
        if (!get(operatorDataLoaded) || get(creationMode) !== 'operator') {
            return;
        }

        const validation = IntervalValidationService.validateTimeInput(
            formData.startTime, 
            formData.duration
        );
        
        if (!validation.isValid) {
            alert(validation.message);
            return;
        }

        const currentIntervals = get(intervals);
        const sameModeOverlap = checkIntervalOverlap(
            currentIntervals,
            formData.startTime,
            formData.duration,
            formData.modeType!
        );
        
        if (sameModeOverlap.overlaps) {
            alert(`Ошибка: интервал пересекается с существующим интервалом\n` +
                  `Время конфликта: ${sameModeOverlap.conflictingInterval?.startTime} - ${sameModeOverlap.conflictingInterval?.endTime}`);
            return;
        }
        
        const tempId = generateTempId();
        const endTime = TimeUtils.calculateEndTimeSeconds(formData.startTime, formData.duration);
        const modeData = createProgramModeData(formData, tempId);
        const timeInterval = createTimeInterval(formData, tempId, endTime);

        createdPrograms.update(current => [...current, { tempId, modeData, timeInterval }]);
        intervals.update(current => [...current, timeInterval]);
        checkAndUpdateAllConflictsForNewInterval(timeInterval);
        updateAllConflicts();
        
        editingInterval.set(null);
        selectedIntervalId.set(null);
        isEditing.set(false);
        syncCurrentProgramWithStore();
    }

    function handleModeFormCancel() {
        editingInterval.set(null);
        selectedIntervalId.set(null);
        isEditing.set(false);
    }

    return {
        handleModeSelect,
        handleModeFormSubmit,
        handleModeFormCancel
    };
}
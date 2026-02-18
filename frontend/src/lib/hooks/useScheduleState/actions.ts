import { WORK_MODES } from '$lib/constants/schedule';
import type {
    ModeCreationForm,
    TimeInterval
} from '$lib/types';
import { AstrocorrectionService } from '$lib/utils/astrocorrection.service';
import { checkIntervalOverlap } from '$lib/utils/interval/conflicts';
import { IntervalValidationService } from '$lib/utils/intervalValidation';
import { TimeUtils } from '$lib/utils/time';
import { get } from 'svelte/store';
import { ScheduleApiService } from '../../../features/services/api/scheduleApi.service';
import { ScheduleCreationService } from '../../../features/services/scheduleCreation.service';
import type { createCreators } from './creators';
import type { createStores } from './stores';
import type { createValidation } from './validation';

export function createActions(
    stores: ReturnType<typeof createStores>,
    creators: ReturnType<typeof createCreators>,
    validation: ReturnType<typeof createValidation>
) {
    const {
        userData,
        creationMode,
        intervals,
        bortData,
        operatorDataLoaded,
        selectedMode,
        createdPrograms,
        editingInterval,
        selectedIntervalId,
        contextDate,
        hasAstrocorrectionData,
        vkiIntervals,
        rotationIntervals,
        isEditing
    } = stores;

    const {
        generateTempId,
        createTimeInterval,
        createUpdatedInterval,
        createProgramModeData
    } = creators;

    const {
        checkAndUpdateAllConflictsForNewInterval,
        updateAllConflicts
    } = validation;

    // Загрузка данных

    async function loadUserData() {
        try {
            const userDataCookie = document.cookie
                .split('; ')
                .find(row => row.startsWith('user_data='));
            
            if (userDataCookie) {
                const userDataStr = userDataCookie.split('=')[1];
                const parsedData = JSON.parse(decodeURIComponent(userDataStr));
                
                const user = {
                    username: parsedData.username,
                    firstName: parsedData.firstName,
                    lastName: parsedData.lastName,
                    enabled: parsedData.enabled !== undefined ? parsedData.enabled : true,
                    accountLocked: parsedData.accountLocked !== undefined ? parsedData.accountLocked : false,
                    failedAttempts: parsedData.failedAttempts || 0,
                    lastLoginAt: parsedData.lastLoginAt,
                    lastLogoutAt: parsedData.lastLogoutAt || '',
                    roles: parsedData.roles || []
                };
                
                userData.set(user);
            }
        } catch (error) {
            console.error('Error parsing user data:', error);
            userData.set(null);
        }
    }

    async function loadBortData(date: string) {
        try {
            const data = await ScheduleApiService.loadBortData(date);
            bortData.set(data);
            console.log('ИД02:', data);
            return data;
        } catch (error) {
            console.error('Ошибка загрузки данных ID02:', error);
            bortData.set(null);
            return null;
        }
    }

    async function loadAstroEvents(date: string) {
        try {
            const [vkiData, rotationData] = await Promise.all([
                ScheduleCreationService.loadVkiData(date),
                ScheduleCreationService.loadRotationData(date)
            ]);
            
            const vkiList = ScheduleCreationService.convertVkiToIntervals(vkiData);
            const rotationList = ScheduleCreationService.convertRotationToIntervals(rotationData, date);
            
            vkiIntervals.set(vkiList);
            rotationIntervals.set(rotationList);

            updateAllConflicts();
        } catch (error) {
            console.error("Ошибка загрузки событий астрокоррекции: ", error);
            vkiIntervals.set([]);
            rotationIntervals.set([]);
        }
    }

    async function checkAndAddAstrocorrection(date: string): Promise<boolean> {
        try {
            const hasAstro = await ScheduleApiService.hasAstrocorrectionData(date);
            hasAstrocorrectionData.set(hasAstro);

            if (get(intervals).length > 0) {
                const intervalsWithAstro = AstrocorrectionService.mergeAstrocorrection(
                    get(intervals),
                    date,
                    hasAstro
                );
                intervals.set(intervalsWithAstro);
                updateAllConflicts();
            }

            return hasAstro;
        } catch (error) {
            console.error("Ошибка при проверке астрокоррекции: ", error);
            hasAstrocorrectionData.set(false);
            return false;
        }
    }

    function setContextDate(date: string) {
        contextDate.set(date);
        loadBortData(date);
    }

    // Обработчики событий

    function handleIntervalClick(interval: TimeInterval) {
        if (interval.isAstrocorrection) {
            alert('Попытка редактировать астрокоррекцию - операция запрещена');
            return;
        }

        isEditing.set(true);

        const intervalWithDefaults = {
            ...interval,
            msu1Config: interval.msu1Config || ScheduleCreationService.getDefaultMsuConfig(),
            msu2Config: interval.msu2Config || ScheduleCreationService.getDefaultMsuConfig(),
            customerCode: interval.customerCode || 1
        };
        
        editingInterval.set(intervalWithDefaults);
        selectedIntervalId.set(interval.id);
        selectedMode.set(interval.mode);
    }

    function handleIntervalDelete(intervalId: string) {
        const currentIntervals = get(intervals);
        const intervalToDelete = currentIntervals.find(i => i.id === intervalId);

        if (intervalToDelete?.isAstrocorrection) {
            alert("Попытка удалить астрокоррекцию - операция запрещена");
            return;
        }

        intervals.set(currentIntervals.filter(interval => interval.id !== intervalId));

        const currentPrograms = get(createdPrograms);
        createdPrograms.set(currentPrograms.filter(program => program.tempId !== intervalId));
        
        const currentEditingInterval = get(editingInterval);
        if (currentEditingInterval?.id === intervalId) {
            const deletedMode = currentEditingInterval.mode;
            editingInterval.set(null);
            selectedIntervalId.set(null);
            selectedMode.set(deletedMode);
            isEditing.set(false);
            handleModeFormCancel();
        }
        
        updateAllConflicts();
    }

    function handleIntervalUpdate(formData: ModeCreationForm) {
        const currentEditingInterval = get(editingInterval);
        if (!currentEditingInterval) return;

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
            currentEditingInterval.mode,
            currentEditingInterval.id
        );
        
        if (sameModeOverlap.overlaps) {
            alert(`Ошибка: интервал пересекается с существующим интервалом\n` +
                  `Время конфликта: ${sameModeOverlap.conflictingInterval?.startTime} - ${sameModeOverlap.conflictingInterval?.endTime}`);
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
                if (program.tempId === currentEditingInterval.id) {
                    const modeData = createProgramModeData(formData, currentEditingInterval.id);
                    return { ...program, modeData, timeInterval: updatedInterval };
                }
                return program;
            })
        );
        
        updateAllConflicts();
        editingInterval.set(null);
        selectedIntervalId.set(null);
        isEditing.set(false);
    }

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
    }

    function handleModeFormCancel() {
        editingInterval.set(null);
        selectedIntervalId.set(null);
        isEditing.set(false);
    }

    // Вспомогательные функции для интерфейса

    function getIntervalColor(interval: TimeInterval): string {
        if (interval.inShadow && interval.willBeSavedInShadow) {
            return '#ff69b4';
        }
        
        if (interval.hasConflict) {
            return '#ff0000';
        }
        
        if (interval.constraintViolations?.length) {
            const onlySpecialViolations = interval.constraintViolations.every(
                v => v.constraintId === 77 || v.constraintId === 78
            );
            return onlySpecialViolations ? '#ff0000' : '#ffffff';
        }

        if (interval.zasvetkaConflict || interval.nearZasvetka) {
            return '#ffffff';
        }
        
        if (interval.isAstrocorrection) {
            return '#1e40af';
        }
        
        return interval.color;
    }

    function getIntervalTitle(interval: TimeInterval): string {
        let title = interval.title || '';

        if (interval.hasConflict) {
            const conflictModes = interval.conflictWith?.map(modeId => {
                const mode = WORK_MODES.find(m => m.id === modeId);
                return mode?.label || `Режим ${modeId}`;
            }).join(', ');
            title += ` (КОНФЛИКТ: ${conflictModes})`;
        }

        if (interval.inShadow) {
            title += interval.willBeSavedInShadow 
                ? ' [В ТЕНИ - БУДЕТ СОХРАНЕНО]'
                : ' [В ТЕНИ - НЕ БУДЕТ СОХРАНЕНО]';
        }
        
        if (!interval.willBeSaved) {
            title += ' [НЕ БУДЕТ СОХРАНЕНО]';
        }
        
        return title;
    }

    return {
        loadUserData,
        loadBortData,
        loadAstroEvents,
        checkAndAddAstrocorrection,
        setContextDate,
        handleIntervalClick,
        handleIntervalDelete,
        handleIntervalUpdate,
        handleModeSelect,
        handleModeFormSubmit,
        handleModeFormCancel,
        getIntervalColor,
        getIntervalTitle
    };
}
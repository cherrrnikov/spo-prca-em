import { CUSTOMER_CODES, WORK_MODES } from '$lib/constants/schedule';

import type { UserResponse } from '$lib/types/auth';
import { MODE_TO_CONSTRAINT_TYPE } from '$lib/types/constraints';
import type {
    CreatedProgramData,
    ForecastData,
    ModeCreationForm,
    OperatorData,
    PpiAssignment,
    ProgramModeData,
    RotationInterval,
    ShadowInterval,
    TimeInterval,
    VkiInterval,
    ZasvetkaInterval
} from '$lib/types/schedule';
import { AstrocorrectionService } from '$lib/utils/astrocorrection.service';
import { IntervalUtils } from '$lib/utils/interval';
import { IntervalValidationService } from '$lib/utils/intervalValidation';
import { ModeUtils } from '$lib/utils/mode';
import { TimeUtils } from '$lib/utils/time';
import { get, writable } from 'svelte/store';
import { ScheduleApiService } from '../../features/services/api/scheduleApi.service';
import { ScheduleCreationService } from '../../features/services/scheduleCreation.service';

export function useScheduleState() {
    const userData = writable<UserResponse | null>(null);
    const creationMode = writable<'operator' | 'reference' | null>(null);
    const intervals = writable<TimeInterval[]>([]);
    const operatorData = writable<OperatorData | null>(null);
    const ppiAssignments = writable<PpiAssignment[]>([]);
    const operatorDataLoaded = writable(false);
    const selectedProgramDate = writable<string>('');
    const forecastData = writable<ForecastData | null>(null);
    const shadowIntervals = writable<ShadowInterval[]>([]);
    const zasvetkaIntervals = writable<ZasvetkaInterval[]>([]);
    const forecastDataLoaded = writable(false);
    const selectedMode = writable<number | null>(null);
    const createdPrograms = writable<CreatedProgramData[]>([]);
    const editingInterval = writable<TimeInterval | null>(null);
    const selectedIntervalId = writable<string | null>(null);
    const contextDate = writable<string>('');
    const hasAstrocorrectionData = writable<boolean>(false);
    const vkiIntervals = writable<VkiInterval[]>([]);
    const rotationIntervals = writable<RotationInterval[]>([]);

    let isEditing = writable(true);

    function loadUserData() {
        try {
            const userDataCookie = document.cookie
                .split('; ')
                .find(row => row.startsWith('user_data='));
            
            if (userDataCookie) {
                const userDataStr = userDataCookie.split('=')[1];
                const parsedData = JSON.parse(decodeURIComponent(userDataStr));
                
                const user: UserResponse = {
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
                console.log('User data loaded:', user);
            }
        } catch (error) {
            console.error('Error parsing user data:', error);
            userData.set(null);
        }
    }

    function setContextDate(date: string) {
        contextDate.set(date);
    }

    function handleIntervalClick(interval: TimeInterval) {
        if (interval.isAstrocorrection) {
            console.log('Попытка редактировать астрокоррекцию - операция запрещена');
            alert('Попытка редактировать астрокоррекцию - операция запрещена');
            return;
        }

        isEditing.set(true);

        const intervalWithDefaults = {
            ...interval,
            msu1Config: interval.msu1Config || getDefaultMsuConfig(),
            msu2Config: interval.msu2Config || getDefaultMsuConfig(),
            customerCode: interval.customerCode || 1
        };
        
        editingInterval.set(intervalWithDefaults);
        selectedIntervalId.set(interval.id);
        selectedMode.set(interval.mode);
    }

    function handleIntervalDelete(intervalId: string) {
        console.log(`Удаление интервала: ${intervalId}`);
        
        const currentIntervals = get(intervals);
        const intervalToDelete = currentIntervals.find(i => i.id === intervalId);

        if (intervalToDelete?.isAstrocorrection) {
            console.warn("Попытка удалить астрокоррекцию - операция запрещена");
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
        
        const sameModeOverlap = IntervalUtils.checkIntervalOverlap(
            currentIntervals,
            formData.startTime,
            formData.duration,
            currentEditingInterval.mode,
            currentEditingInterval.id
        );
        
        if (sameModeOverlap.overlaps) {
            alert(`Ошибка: интервал пересекается с существующим интервалом\n` +
                  `Время конфликта: ${sameModeOverlap.conflictingInterval?.startTime} - ${sameModeOverlap.conflictingInterval?.endTime}\n` +
                  `Попробуйте выбрать другое время или уменьшить длительность.`);
            return;
        }
        
        const endTime =  TimeUtils.calculateEndTimeSeconds(formData.startTime, formData.duration);
        
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
        
        const sameModeOverlap = IntervalUtils.checkIntervalOverlap(
            currentIntervals,
            formData.startTime,
            formData.duration,
            formData.modeType!
        );
        
        if (sameModeOverlap.overlaps) {
            alert(`Ошибка: интервал пересекается с существующим интервалом\n` +
                `Время конфликта: ${sameModeOverlap.conflictingInterval?.startTime} - ${sameModeOverlap.conflictingInterval?.endTime}\n` +
                `Попробуйте выбрать другое время или уменьшить длительность.`);
            return;
        }
        
        const tempId = generateTempId();
        const endTime = TimeUtils.calculateEndTimeSeconds(formData.startTime, formData.duration);
        const modeData = createProgramModeData(formData, tempId);
        const timeInterval = createTimeInterval(formData, tempId, endTime);

        console.log('Создан новый интервал:', timeInterval);
        console.log('Созданные данные для API:', modeData);

        createdPrograms.update(current => [...current, {
            tempId,
            modeData,
            timeInterval
        }]);
        
        intervals.update(current => [...current, timeInterval]);
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

    function createUpdatedInterval(
        editingInterval: TimeInterval, 
        formData: ModeCreationForm, 
        endTime: string
    ): TimeInterval {
        const currentContextDate = get(contextDate);

        const updatedInterval: TimeInterval = {
            ...editingInterval,
            date: currentContextDate,
            startTime: formData.startTime,
            endTime: endTime, 
            ppi: formData.ppiNum,
            dlit: formData.duration,
            city: ScheduleCreationService.getCityByPpi(formData.ppiNum),
            color: ScheduleCreationService.getColorByPpi(formData.ppiNum),
        };
        
        if (formData.modeType === 7 && formData.kvdConfig) {
            updatedInterval.kvdConfig = { ...formData.kvdConfig };
        }
        
        if (formData.modeType === 8) {
            updatedInterval.msu1Config = { ...formData.msu1Config };
            updatedInterval.msu2Config = { ...formData.msu2Config };
        }

        if (formData.modeType === 6) {
            updatedInterval.nOna = formData.nOna || editingInterval.nOna || 1;
        }

        return updatedInterval;
    }

    function createTimeInterval(
        formData: ModeCreationForm, 
        tempId: string, 
        endTime: string
    ): TimeInterval {
        const currentContextDate = get(contextDate);

        const interval: TimeInterval = {
            id: tempId,
            mode: formData.modeType!,
            date: currentContextDate,
            startTime: formData.startTime,
            endTime: endTime, 
            city: ScheduleCreationService.getCityByPpi(formData.ppiNum),
            color: ScheduleCreationService.getColorByPpi(formData.ppiNum),
            title: ModeUtils.getModeTitle(formData.modeType!),
            ppi: formData.ppiNum,
            dlit: formData.duration,
            customerCode: formData.customerCode,
            hasConflict: false,
            conflictWith: [],
            nearZasvetka: false,
            zasvetkaConflict: false,
            zasvetkaDistance: 0,
            willBeSaved: true,
        };

        if (formData.modeType === 7 && formData.kvdConfig) {
            interval.kvdConfig = { ...formData.kvdConfig };
        }
        
        if (formData.modeType === 8) {
            interval.msu1Config = { ...formData.msu1Config };
            interval.msu2Config = { ...formData.msu2Config };
        }

        if (formData.modeType === 6) {
            interval.nOna = formData.nOna || 1;
        }

        checkAndUpdateAllConflictsForNewInterval(interval);
        return interval;
    }

    function checkAndUpdateAllConflictsForNewInterval(newInterval: TimeInterval) {
        const currentIntervals = get(intervals);
        const currentOperatorData = get(operatorData);
        const currentPpiAssignments = get(ppiAssignments);
        const currentZasvetkaIntervals = get(zasvetkaIntervals);

        const nonAstroIntervals = currentIntervals.filter(i => !i.isAstrocorrection);

        const allIntervals = [
            ...nonAstroIntervals,
            ...(currentOperatorData ? 
                ScheduleCreationService.convertToTimeIntervals(
                    currentOperatorData, 
                    currentPpiAssignments, 
                    WORK_MODES
                ) : 
                [])
        ];
        
        let hasConflict = false;
        let hasAstroConflict = false;
        const astroConflictWith: number[] = [];
        const conflictWith: number[] = [];
        
        for (const existingInterval of allIntervals) {
            if (existingInterval.mode === newInterval.mode) {
                continue;
            }
            
            const overlap = IntervalUtils.checkTwoIntervalsOverlap(
                newInterval.startTime,
                newInterval.endTime,
                existingInterval.startTime,
                existingInterval.endTime
            );
            
            if (overlap) {
                hasConflict = true;
                if (!conflictWith.includes(existingInterval.mode)) {
                    conflictWith.push(existingInterval.mode);
                }
            }
        }
        
        if (!newInterval.isAstrocorrection) {
            const astroIntervals = currentIntervals.filter(i => i.isAstrocorrection);
            
            for (const astroInterval of astroIntervals) {
                const overlap = IntervalUtils.checkTwoIntervalsOverlap(
                    newInterval.startTime,
                    newInterval.endTime,
                    astroInterval.startTime,
                    astroInterval.endTime
                );
                
                if (overlap) {
                    hasAstroConflict = true;
                    if (!astroConflictWith.includes(astroInterval.mode)) {
                        astroConflictWith.push(astroInterval.mode);
                    }
                    
                    console.log('Конфликт с астрокоррекцией:', {
                        newInterval: `${newInterval.startTime}-${newInterval.endTime}`,
                        astroInterval: `${astroInterval.startTime}-${astroInterval.endTime}`,
                        mode: newInterval.mode
                    });
                }
            }
        }

        newInterval.hasConflict = hasConflict;
        newInterval.conflictWith = conflictWith;
        newInterval.hasAstroConflict = hasAstroConflict;
        newInterval.astroConflictWith = astroConflictWith;
        
        const zasvetkaCheck = IntervalUtils.checkZasvetkaProximity(
            newInterval.startTime,
            newInterval.endTime,
            currentZasvetkaIntervals
        );
        
        newInterval.nearZasvetka = zasvetkaCheck.nearZasvetka;
        newInterval.zasvetkaConflict = zasvetkaCheck.zasvetkaConflict;
        newInterval.zasvetkaDistance = zasvetkaCheck.minDistance;
        
        newInterval.willBeSaved = 
            !hasConflict && 
            !hasAstroConflict &&
            !zasvetkaCheck.zasvetkaConflict && 
            !zasvetkaCheck.nearZasvetka;
    }

    async function loadAstroEvents(date: string) {
        try {
            console.log('Загрузка ВКИ для даты:', date);
            
            const [vkiData, rotationData] = await Promise.all([
                ScheduleCreationService.loadVkiData(date),
                ScheduleCreationService.loadRotationData(date)
            ]);
            
            console.log('Получены данные ВКИ:', vkiData);
            
            const vkiList = ScheduleCreationService.convertVkiToIntervals(vkiData);
            const rotationList = ScheduleCreationService.convertRotationToIntervals(rotationData, date);
            
            console.log('Преобразованные ВКИ интервалы:', vkiList);
            
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

    function updateAllConflicts() {
        const currentIntervals = get(intervals);
        const currentZasvetka = get(zasvetkaIntervals) || [];
        const currentShadows = get(shadowIntervals) || [];
        const currentVki = get(vkiIntervals) || [];
        const currentRotations = get(rotationIntervals) || [];
        
        const intervalsWithConflicts = IntervalUtils.checkAllConflicts(
            currentIntervals,
            currentZasvetka,
            currentShadows,
            currentVki,
            currentRotations 
        );
        
        intervals.set(
            currentIntervals.map(interval => {
                const updatedInterval = intervalsWithConflicts.find(i => i.id === interval.id);
                if (updatedInterval) {
                    return {
                        ...interval,
                        hasConflict: updatedInterval.hasConflict,
                        conflictWith: updatedInterval.conflictWith,
                        hasAstroConflict: updatedInterval.hasAstroConflict,
                        astroConflictWith: updatedInterval.astroConflictWith,
                        nearZasvetka: updatedInterval.nearZasvetka,
                        zasvetkaConflict: updatedInterval.zasvetkaConflict,
                        zasvetkaDistance: updatedInterval.zasvetkaDistance,
                        inShadow: updatedInterval.inShadow,
                        willBeSavedInShadow: updatedInterval.willBeSavedInShadow,
                        shadowPriority: updatedInterval.shadowPriority,
                        willBeSaved: updatedInterval.willBeSaved,
                        constraintViolations: updatedInterval.constraintViolations
                    };
                }
                return interval;
            })
        );
    }

    function createProgramModeData(formData: ModeCreationForm, tempId: string): ProgramModeData {
        const currentOperatorData = get(operatorData);
        const currentContextDate = get(contextDate);
        const mainId = currentOperatorData?.main.id || 0;
        const endDisplayTime = TimeUtils.calculateEndTimeSeconds(formData.startTime, formData.duration);
        const dateOff = `${currentContextDate}T${endDisplayTime}`;
        const dateOn = `${currentContextDate}T${formData.startTime}`; 

        const baseData = {
            numRp: 0,
            numKa: currentOperatorData?.main.n_ka || 1,
            dateOn: dateOn,
            dateOff: dateOff,
            kodMode: formData.modeType!,
            numPpi: formData.ppiNum,
            dlit: formData.duration,
            zakazchik: getCustomerLabel(formData.customerCode)
        };
        
        if (formData.modeType === 7) {
            const kvdConfig = formData.kvdConfig || {
                prMsu: 0,
                prBssd: 0,
                prZg: 0
            };
            
            return {
                ...baseData,
                kvdData: {
                    id: 0,
                    idMain: mainId,
                    prMsu: kvdConfig.prMsu,
                    prBssd: kvdConfig.prBssd,
                    prZg: kvdConfig.prZg
                }
            };
        } else if (formData.modeType === 8) {
            const msu1Config = formData.msu1Config || getDefaultMsuConfig();
            const msu2Config = formData.msu2Config || getDefaultMsuConfig();
            
            return {
                ...baseData,
                tsData: {
                    id: 0,
                    idMain: mainId,
                    tip: 1,
                    reg: 1,
                    dlit: baseData.dlit,
                    prMsu1: msu1Config.prMsu || 0,
                    vd1Msu1: msu1Config.vd1 || 0,
                    vd2Msu1: msu1Config.vd2 || 0,
                    vd3Msu1: msu1Config.vd3 || 0,
                    ik4Msu1: msu1Config.ik4 || 0,
                    ik5Msu1: msu1Config.ik5 || 0,
                    ik6Msu1: msu1Config.ik6 || 0,
                    ik7Msu1: msu1Config.ik7 || 0,
                    ik8Msu1: msu1Config.ik8 || 0,
                    ik9Msu1: msu1Config.ik9 || 0,
                    ik10Msu1: msu1Config.ik10 || 0,
                    prMsu2: msu2Config.prMsu || 0,
                    vd1Msu2: msu1Config.vd1 || 0,
                    vd2Msu2: msu1Config.vd2 || 0,
                    vd3Msu2: msu1Config.vd3 || 0,
                    ik4Msu2: msu1Config.ik4 || 0,
                    ik5Msu2: msu1Config.ik5 || 0,
                    ik6Msu2: msu1Config.ik6 || 0,
                    ik7Msu2: msu1Config.ik7 || 0,
                    ik8Msu2: msu1Config.ik8 || 0,
                    ik9Msu2: msu1Config.ik9 || 0,
                    ik10Msu2: msu1Config.ik10 || 0,
                    prBssd: 1,
                    prZg: 1,
                    prOtklZgBssd: 0
                }
            };
        } else if (formData.modeType === 4) {
            return {
                ...baseData,
                tnpData: {
                    id: 0,
                    idMain: mainId,
                    prMsu: 1,
                    prBssd: 1,
                    prZg: 1
                }
            };
        } else if (formData.modeType === 6) {
            return {
                ...baseData,
                onaData: {
                    id: 0,
                    idMain: mainId,
                    typeOmi: 1,
                    dN: dateOn,
                    dK: dateOff,
                    nOna: formData.nOna || 1,
                    nPpi: formData.ppiNum
                }
            };
        } else {
            return baseData;
        }
    }

    function getCustomerLabel(code: number): string {
        const customer = CUSTOMER_CODES.find(c => c.value === code);
        return customer?.label.split(' - ')[1] || '';
    }

    function generateTempId(): string {
        return `created_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    }

    function getDefaultMsuConfig() {
        return ScheduleCreationService.getDefaultMsuConfig();
    }

    function getIntervalColor(interval: TimeInterval): string {
        if (interval.constraintViolations && interval.constraintViolations.length > 0) {
            console.log(`🔴 Интервал с нарушениями:`, {
                id: interval.id,
                mode: interval.mode,
                type: MODE_TO_CONSTRAINT_TYPE[interval.mode],
                violations: interval.constraintViolations.length,
                willBeSaved: interval.willBeSaved
            });
            return '#ffffff';
        }

        if (interval.isAstrocorrection) {
            return '#1e40af'; 
        }

        if (interval.inShadow && interval.willBeSavedInShadow) {
            return '#ff69b4'; 
        }
        if (interval.zasvetkaConflict || interval.nearZasvetka || interval.hasAstroConflict || (interval.constraintViolations && interval.constraintViolations.length > 0)) {
            return '#ffffff';
        }
        if (interval.hasConflict) {
            return '#ff0000';
        }

        return interval.color;
    }

    function getIntervalTitle(interval: TimeInterval): string {
        let title = interval.title || '';
        
        if (interval.hasAstroConflict) {
            title += ` (КОНФЛИКТ С АСТРОКОРРЕКЦИЕЙ)`;
        }

        if (interval.hasConflict) {
            const conflictModes = interval.conflictWith?.map(modeId => {
                const mode = WORK_MODES.find(m => m.id === modeId);
                return mode?.label || `Режим ${modeId}`;
            }).join(', ');
            
            title += ` (КОНФЛИКТ: ${conflictModes})`;
        }

        if (interval.inShadow) {
            if (interval.willBeSavedInShadow) {
                title += ' [В ТЕНИ - БУДЕТ СОХРАНЕНО]';
            } else {
                title += ' [В ТЕНИ - НЕ БУДЕТ СОХРАНЕНО]';
            }
        }
        
        if (!interval.willBeSaved) {
            title += ' [НЕ БУДЕТ СОХРАНЕНО]';
        }
        
        return title;
    }

    return {
        userData,
        creationMode,
        intervals,
        operatorData,
        ppiAssignments,
        operatorDataLoaded,
        selectedProgramDate,
        forecastData,
        shadowIntervals,
        zasvetkaIntervals,
        forecastDataLoaded,
        selectedMode,
        createdPrograms,
        editingInterval,
        selectedIntervalId,
        contextDate,
        hasAstrocorrectionData,
        vkiIntervals,
        rotationIntervals,
        isEditing,
        
        loadUserData,
        handleIntervalClick,
        handleIntervalDelete,
        handleIntervalUpdate,
        handleModeSelect,
        handleModeFormSubmit,
        handleModeFormCancel,
        loadAstroEvents,
        checkAndAddAstrocorrection,
        
        getIntervalColor,
        getIntervalTitle,
        setContextDate
    };
}
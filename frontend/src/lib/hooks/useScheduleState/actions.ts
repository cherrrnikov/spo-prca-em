import { WORK_MODES } from '$lib/constants/schedule';
import type {
    CreatedProgramData,
    ModeCreationForm,
    ProgramModeData,
    RotationInterval,
    ShadowInterval,
    TimeInterval,
    VkiInterval,
    ZasvetkaInterval
} from '$lib/types';
import type { ProgramsListItem } from '$lib/types/analysis';
import { AstrocorrectionService } from '$lib/utils/astrocorrection.service';
import { checkAllConflicts } from '$lib/utils/interval';
import { checkIntervalOverlap } from '$lib/utils/interval/conflicts';
import { IntervalValidationService } from '$lib/utils/intervalValidation';
import { TimeUtils } from '$lib/utils/time';
import { TooltipFormatter } from '$lib/utils/tooltipFormatter';
import { get } from 'svelte/store';
import { ScheduleApiService } from '../../../features/services/api/scheduleApi.service';
import { ScheduleConverterService } from '../../../features/services/data/scheduleConverter.service';
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
        operatorData,
        operatorDataLoaded,
        selectedMode,
        createdPrograms,
        editingInterval,
        selectedIntervalId,
        contextDate,
        hasAstrocorrectionData,
        vkiIntervals,
        rotationIntervals,
        isEditing,
        ppiAssignments,          
        shadowIntervals,        
        zasvetkaIntervals,     
        programsList,            
        activeProgramId,         
        isAnalysisMode,          
        analysisModal,
        selectedProgramDate  
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
        createdPrograms.set(currentPrograms.filter(program => program.timeInterval.id !== intervalId));
        
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
        syncCurrentProgramWithStore();
    }

    function handleIntervalUpdate(formData: ModeCreationForm) {
        console.log(`✏️ handleIntervalUpdate для интервала:`, {
            editingId: get(editingInterval)?.id,
            formData: formData
        });

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
                if (program.timeInterval.id === currentEditingInterval.id) {
                    const modeData = createProgramModeData(formData, program.tempId); // сохраняем тот же tempId
                    console.log(`  📝 Обновляем programData:`, {
                        old: program.modeData,
                        new: modeData
                    });
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

    // Сохранение текущей ПРЦА в список для анализа 
    function saveCurrentProgramToAnalysis() {
        const currentBortData = get(bortData);
        const currentIntervals = get(intervals);
        const currentDate = get(selectedProgramDate);
        const currentOperator = get(operatorData);
        const currentPpi = get(ppiAssignments);
        const currentCreated = get(createdPrograms);
        const currentShadows = get(shadowIntervals);
        const currentZasvetki = get(zasvetkaIntervals);
        const currentVki = get(vkiIntervals);
        const currentRotations = get(rotationIntervals);

        // Генерируем имя для ПРЦА
        const programName = `ПРЦА ${TimeUtils.formatDate(currentDate)}`;
        
        const programItem: ProgramsListItem = {
            id: `program_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
            name: programName,
            date: currentDate,
            intervals: [...currentIntervals],
            operatorData: currentOperator ? { ...currentOperator } : null,
            bortData: currentBortData ? {...currentBortData} : null,
            ppiAssignments: [...currentPpi],
            createdPrograms: [...currentCreated],
            shadowIntervals: [...currentShadows],
            zasvetkaIntervals: [...currentZasvetki],
            vkiIntervals: [...currentVki],
            rotationIntervals: [...currentRotations]
        };
        
        programsList.update(list => [...list, programItem]);
        activeProgramId.set(programItem.id);
        isAnalysisMode.set(true);
        
        openAnalysisModal(currentDate);
    }

    // Открыть модальное окно выбора периода
    function openAnalysisModal(date?: string) {
        const currentDate = date || get(selectedProgramDate) || get(contextDate);

        console.log("openAnalysisModal with date:", currentDate); 

        analysisModal.set({
            isOpen: true,
            startDate: currentDate,
            endDate: currentDate,
            isLoading: false
        });
    }

    // Закрыть модальное окно
    function closeAnalysisModal() {
        analysisModal.update(modal => ({ ...modal, isOpen: false }));
    }

    // Создать анализ (копировать на диапазон д ат)
    async function createAnalysis(startDate: string, endDate: string) {
        analysisModal.update(modal => ({ ...modal, isLoading: true }));
        
        try {
            const currentProgram = get(programsList).find(p => p.id === get(activeProgramId));
            if (!currentProgram) throw new Error("Нет активной ПРЦА");
            
            console.log("Исходная ПРЦА:", currentProgram.name);
            console.log("Диапазон дат:", startDate, "→", endDate);
            
            const dates = TimeUtils.generateDateRange(startDate, endDate);
            console.log("Даты для копирования:", dates);
            
            const newPrograms: ProgramsListItem[] = [];
            
            for (const date of dates) {
                // Пропускаем исходную дату (она уже есть)
                if (date === currentProgram.date) continue;
                
                console.log(`\n--- Обработка даты: ${date} ---`);
                
                // Загружаем ВСЕ данные для этой даты
                let operatorDataForDate = null;
                let bortDataForDate = null;
                let forecastDataForDate = null;
                let vkiDataForDate = null;
                let rotationDataForDate = null;
                
                try {
                    // Загружаем все данные параллельно
                    const [operator, bort, forecast, vki, rotation] = await Promise.allSettled([
                        ScheduleApiService.loadOperatorData(date).catch(() => null),
                        ScheduleApiService.loadBortData(date).catch(() => null),
                        ScheduleApiService.loadForecastData(date).catch(() => null),
                        ScheduleApiService.loadVkiData(date).catch(() => null),
                        ScheduleApiService.loadRotationData(date).catch(() => null)
                    ]);
                    
                    operatorDataForDate = operator.status === 'fulfilled' ? operator.value : null;
                    bortDataForDate = bort.status === 'fulfilled' ? bort.value : null;
                    forecastDataForDate = forecast.status === 'fulfilled' ? forecast.value : null;
                    vkiDataForDate = vki.status === 'fulfilled' ? vki.value : null;
                    rotationDataForDate = rotation.status === 'fulfilled' ? rotation.value : null;
                    
                } catch (error) {
                    console.warn(`Ошибка при загрузке данных для ${date}:`, error);
                }
                
                // Конвертируем прогнозные данные в интервалы
                let shadowsForDate: ShadowInterval[] = [];
                let zasvetkiForDate: ZasvetkaInterval[] = [];
                
                if (forecastDataForDate) {
                    const forecast = ScheduleConverterService.convertForecastToIntervals(forecastDataForDate);
                    shadowsForDate = forecast.shadows;
                    zasvetkiForDate = forecast.zasvetki;
                }
                
                // Конвертируем астрособытия
                let vkiForDate: VkiInterval[] = [];
                let rotationsForDate: RotationInterval[] = [];
                
                if (vkiDataForDate) {
                    vkiForDate = ScheduleConverterService.convertVkiToIntervals(vkiDataForDate);
                }
                
                if (rotationDataForDate) {
                    rotationsForDate = ScheduleConverterService.convertRotationToIntervals(rotationDataForDate, date);
                }
                
                let intervalsForDate: TimeInterval[] = [];
                let createdProgramsForDate: CreatedProgramData[] = [];
                
                if (operatorDataForDate) {
                    console.log(`✅ Есть данные ИД06 для ${date}, создаём интервалы из них`);
                    
                    // Проверяем, какие типы режимов есть в ИД06
                    const hasKvd = operatorDataForDate.kvd_list && operatorDataForDate.kvd_list.length > 0;
                    const hasTnp = operatorDataForDate.tnp_list && operatorDataForDate.tnp_list.length > 0;
                    const hasTs = operatorDataForDate.ts_list && operatorDataForDate.ts_list.length > 0;
                    const hasOna = operatorDataForDate.ona_list && operatorDataForDate.ona_list.length > 0;
                    
                    console.log(`  📊 Типы в ИД06: КВД:${hasKvd}, ТНП:${hasTnp}, ТС:${hasTs}, ОНА:${hasOna}`);
                    
                    // Создаём интервалы из ИД06
                    const intervalsFromId06 = ScheduleCreationService.convertToTimeIntervals(
                        operatorDataForDate,
                        currentProgram.ppiAssignments,
                        WORK_MODES
                    );
                    
                    // Начинаем с интервалов из ИД06
                    intervalsForDate = [...intervalsFromId06];
                    
                    // Создаём Set существующих ID для быстрого поиска
                    const existingIds = new Set(intervalsFromId06.map(i => i.id));
                    
                    // Добавляем недостающие типы из исходной ПРЦА
                    currentProgram.intervals.forEach(interval => {
                        const mode = interval.mode;
                        
                        // Пропускаем астрокоррекции (они будут добавлены позже)
                        if (interval.isAstrocorrection) return;
                        
                        // Определяем, нужно ли копировать этот интервал
                        let shouldCopy = false;
                        let typeName = '';
                        
                        if (mode === 7 && !hasKvd) {        // КВД
                            shouldCopy = true;
                            typeName = 'КВД';
                        } else if (mode === 4 && !hasTnp) { // ТНП
                            shouldCopy = true;
                            typeName = 'ТНП';
                        } else if (mode === 8 && !hasTs) {  // ТС
                            shouldCopy = true;
                            typeName = 'ТС';
                        } else if (mode === 6 && !hasOna) { // ОНА
                            shouldCopy = true;
                            typeName = 'ОНА';
                        }
                        
                        if (shouldCopy) {
                            console.log(`  ➕ Копируем ${typeName} из исходной ПРЦА`);
                            
                            const newInterval = {
                                ...interval,
                                id: `${interval.id}_${date.replace(/-/g, '')}`,
                                date: date
                            };
                            
                            // Проверяем, не добавили ли уже такой интервал
                            if (!existingIds.has(newInterval.id)) {
                                intervalsForDate.push(newInterval);
                                existingIds.add(newInterval.id);
                                
                                // Также копируем соответствующий createdProgram
                                const originalProgram = currentProgram.createdPrograms.find(p => 
                                    p.timeInterval.id === interval.id
                                );
                                
                                if (originalProgram) {
                                    createdProgramsForDate.push({
                                        ...originalProgram,
                                        tempId: `${originalProgram.tempId}_${date.replace(/-/g, '')}`,
                                        timeInterval: newInterval
                                    });
                                }
                            }
                        }
                    });
                    
                    // Создаём createdPrograms из ИД06
                    const mainId = operatorDataForDate.main?.id || 0;
                    const numKa = operatorDataForDate.main?.n_ka || 1;
                    
                    // КВД
                    if (operatorDataForDate.kvd_list) {
                        operatorDataForDate.kvd_list.forEach((kvd: any) => {
                            const assignment = currentProgram.ppiAssignments.find(a => 
                                a.recordId === kvd.id && a.recordType === 'kvd'
                            );
                            
                            if (assignment) {
                                const modeData: ProgramModeData = {
                                    numRp: 0,
                                    numKa: numKa,
                                    dateOn: kvd.dn,
                                    dateOff: kvd.dk,
                                    kodMode: 7,
                                    numPpi: assignment.ppiNum,
                                    dlit: TimeUtils.calculateDuration(kvd.dn, kvd.dk),
                                    kvdData: {
                                        id: kvd.id,
                                        idMain: mainId,
                                        prMsu: kvd.prMsu,
                                        prBssd: kvd.prBssd,
                                        prZg: kvd.prZg
                                    }
                                };
                                
                                const tempId = `kvd_${kvd.id}_${date.replace(/-/g, '')}`;
                                const timeInterval = intervalsForDate.find(i => 
                                    i.id.includes(`kvd_${kvd.id}`)
                                );
                                
                                if (timeInterval) {
                                    createdProgramsForDate.push({
                                        tempId,
                                        modeData,
                                        timeInterval
                                    });
                                }
                            }
                        });
                    }
                    
                    // ТНП
                    if (operatorDataForDate.tnp_list) {
                        operatorDataForDate.tnp_list.forEach((tnp: any) => {
                            const assignment = currentProgram.ppiAssignments.find(a => 
                                a.recordId === tnp.id && a.recordType === 'tnp'
                            );
                            
                            if (assignment) {
                                const modeData: ProgramModeData = {
                                    numRp: 0,
                                    numKa: numKa,
                                    dateOn: tnp.dn,
                                    dateOff: tnp.dk,
                                    kodMode: 4,
                                    numPpi: assignment.ppiNum,
                                    dlit: tnp.dlit,
                                    tnpData: {
                                        id: tnp.id,
                                        idMain: mainId,
                                        prMsu: tnp.prMsu,
                                        prBssd: tnp.prBssd,
                                        prZg: tnp.prZg
                                    }
                                };
                                
                                const tempId = `tnp_${tnp.id}_${date.replace(/-/g, '')}`;
                                const timeInterval = intervalsForDate.find(i => 
                                    i.id.includes(`tnp_${tnp.id}`)
                                );
                                
                                if (timeInterval) {
                                    createdProgramsForDate.push({
                                        tempId,
                                        modeData,
                                        timeInterval
                                    });
                                }
                            }
                        });
                    }
                    
                    // ТС (особая обработка - разбиваем на подынтервалы)
                    if (operatorDataForDate.ts_list) {
                        for (const ts of operatorDataForDate.ts_list) {
                            const assignment = currentProgram.ppiAssignments.find(a => 
                                a.recordId === ts.id && a.recordType === 'ts'
                            );
                            
                            if (assignment) {
                                // Находим все подынтервалы для этого ТС
                                const tsSubIntervals = intervalsForDate.filter(i => 
                                    i.id.startsWith(`ts_${ts.id}`)
                                );
                                
                                tsSubIntervals.forEach((subInterval, idx) => {
                                    const modeData: ProgramModeData = {
                                        numRp: 0,
                                        numKa: numKa,
                                        dateOn: `${date}T${subInterval.startTime}`,
                                        dateOff: `${date}T${subInterval.endTime}`,
                                        kodMode: 8,
                                        numPpi: assignment.ppiNum,
                                        dlit: subInterval.dlit || 420,
                                        tsData: {
                                            id: ts.id,
                                            idMain: mainId,
                                            tip: ts.tip,
                                            reg: ts.reg,
                                            dlit: subInterval.dlit || 420,
                                            prMsu1: ts.pr_msu1,
                                            vd1Msu1: ts.pr_vd1_1,
                                            vd2Msu1: ts.pr_vd2_1,
                                            vd3Msu1: ts.pr_vd3_1,
                                            ik4Msu1: ts.pr_ik4_1,
                                            ik5Msu1: ts.pr_ik5_1,
                                            ik6Msu1: ts.pr_ik6_1,
                                            ik7Msu1: ts.pr_ik7_1,
                                            ik8Msu1: ts.pr_ik8_1,
                                            ik9Msu1: ts.pr_ik9_1,
                                            ik10Msu1: ts.pr_ik10_1,
                                            prMsu2: ts.pr_msu2,
                                            vd1Msu2: ts.pr_vd1_2,
                                            vd2Msu2: ts.pr_vd2_2,
                                            vd3Msu2: ts.pr_vd3_2,
                                            ik4Msu2: ts.pr_ik4_2,
                                            ik5Msu2: ts.pr_ik5_2,
                                            ik6Msu2: ts.pr_ik6_2,
                                            ik7Msu2: ts.pr_ik7_2,
                                            ik8Msu2: ts.pr_ik8_2,
                                            ik9Msu2: ts.pr_ik9_2,
                                            ik10Msu2: ts.pr_ik10_2,
                                            prBssd: 0,
                                            prZg: 0,
                                            prOtklZgBssd: ts.pr_otkl_zg
                                        }
                                    };
                                    
                                    const tempId = `ts_${ts.id}_${idx}_${date.replace(/-/g, '')}`;
                                    createdProgramsForDate.push({
                                        tempId,
                                        modeData,
                                        timeInterval: subInterval
                                    });
                                });
                            }
                        }
                    }
                    
                    // ОНА
                    if (operatorDataForDate.ona_list) {
                        operatorDataForDate.ona_list.forEach((ona: any) => {
                            const assignment = currentProgram.ppiAssignments.find(a => 
                                a.recordId === ona.id && a.recordType === 'ona'
                            );
                            
                            if (assignment) {
                                const modeData: ProgramModeData = {
                                    numRp: 0,
                                    numKa: numKa,
                                    dateOn: ona.dn,
                                    dateOff: ona.dk,
                                    kodMode: 6,
                                    numPpi: assignment.ppiNum,
                                    dlit: ona.dlit,
                                    onaData: {
                                        id: ona.id,
                                        idMain: ona.id_main,
                                        typeOmi: ona.typeOmi,
                                        dN: ona.dn,
                                        dK: ona.dk,
                                        nOna: ona.n_ona,
                                        nPpi: assignment.ppiNum
                                    }
                                };
                                
                                const tempId = `ona_${ona.id}_${date.replace(/-/g, '')}`;
                                const timeInterval = intervalsForDate.find(i => 
                                    i.id.includes(`ona_${ona.id}`)
                                );
                                
                                if (timeInterval) {
                                    createdProgramsForDate.push({
                                        tempId,
                                        modeData,
                                        timeInterval
                                    });
                                }
                            }
                        });
                    }
                    
                } else {
                    console.log(`⚠️ Нет данных ИД06 для ${date}, копируем всё из исходной ПРЦА`);
                    
                    intervalsForDate = currentProgram.intervals.map(interval => ({
                        ...interval,
                        id: `${interval.id}_${date.replace(/-/g, '')}`,
                        date: date
                    }));
                    
                    createdProgramsForDate = currentProgram.createdPrograms.map(p => ({
                        ...p,
                        tempId: `${p.tempId}_${date.replace(/-/g, '')}`,
                        timeInterval: {
                            ...p.timeInterval,
                            id: `${p.timeInterval.id}_${date.replace(/-/g, '')}`,
                            date: date
                        }
                    }));
                }
                
                // --- АСТРОКОРРЕКЦИИ ---
                // Определяем режим астрокоррекции для этой даты
                let isFullAstroMode = false;
                
                // Проверяем наличие ВКИ
                if (vkiDataForDate && vkiDataForDate.impulses && vkiDataForDate.impulses.length > 0) {
                    isFullAstroMode = true;
                    console.log(`📡 Есть данные ВКИ для ${date}, полный режим астрокоррекции`);
                }
                
                // Проверяем, попадает ли дата в период сезонного разворота
                if (rotationDataForDate && rotationDataForDate.rotations) {
                    for (const rotation of rotationDataForDate.rotations) {
                        const rotationStart = rotation.data_n.split('T')[0];
                        const rotationEnd = rotation.data_k ? rotation.data_k.split('T')[0] : rotationStart;
                        
                        if (date >= rotationStart && date <= rotationEnd) {
                            isFullAstroMode = true;
                            console.log(`🔄 Дата ${date} входит в период разворота (${rotationStart} - ${rotationEnd}), полный режим астрокоррекции`);
                            break;
                        }
                    }
                }
                
                // Добавляем астрокоррекции
                intervalsForDate = AstrocorrectionService.mergeAstrocorrection(
                    intervalsForDate,
                    date,
                    isFullAstroMode
                );
                
                console.log(`✨ Добавлено астрокоррекций: ${isFullAstroMode ? '6 (полный режим)' : '2 (обычный режим)'}`);
                
                // Проверяем конфликты с новыми тенями/засветками/астрособытиями
                const intervalsWithConflicts = checkAllConflicts(
                    intervalsForDate,
                    zasvetkiForDate,
                    shadowsForDate,
                    vkiForDate,
                    rotationsForDate
                );
                
                // Создаём запись ПРЦА для этой даты
                newPrograms.push({
                    id: `program_${date.replace(/-/g, '')}_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
                    name: `ПРЦА ${TimeUtils.formatDate(date)}`,
                    date: date,
                    intervals: intervalsWithConflicts,
                    operatorData: operatorDataForDate,
                    bortData: bortDataForDate,
                    ppiAssignments: [...currentProgram.ppiAssignments],
                    createdPrograms: createdProgramsForDate,
                    shadowIntervals: shadowsForDate,
                    zasvetkaIntervals: zasvetkiForDate,
                    vkiIntervals: vkiForDate,
                    rotationIntervals: rotationsForDate
                });
            }
            
            // Добавляем все созданные ПРЦА в список
            programsList.update(list => [...list, ...newPrograms]);
            
            console.log(`\n=== ИТОГО: создано ${newPrograms.length} ПРЦА для анализа ===`);
            alert(`Создано ${newPrograms.length} ПРЦА для анализа`);
            
        } catch (error) {
            console.error("Ошибка при создании анализа:", error);
        } finally {
            analysisModal.update(modal => ({ ...modal, isLoading: false, isOpen: false }));
        }
    }

    // Переключение между ПРЦА
    function selectProgram(programId: string) {
        syncCurrentProgramWithStore();

        const program = get(programsList).find(p => p.id === programId);
        if (!program) return;
        
        // Устанавливаем все данные выбранной ПРЦА
        intervals.set(program.intervals);
        operatorData.set(program.operatorData);
        ppiAssignments.set(program.ppiAssignments);
        createdPrograms.set(program.createdPrograms);
        shadowIntervals.set(program.shadowIntervals);
        zasvetkaIntervals.set(program.zasvetkaIntervals);
        vkiIntervals.set(program.vkiIntervals);
        rotationIntervals.set(program.rotationIntervals);
        contextDate.set(program.date);
        activeProgramId.set(programId);
    }

    // Синхронизировать изменения с активной ПРЦА
    function syncCurrentProgramWithStore() {
        const currentActiveId = get(activeProgramId);
        if (!currentActiveId) return; // не в режиме анализа
        
        const currentIntervals = get(intervals);
        const currentOperator = get(operatorData);
        const currentPpi = get(ppiAssignments);
        const currentCreated = get(createdPrograms);
        const currentShadows = get(shadowIntervals);
        const currentZasvetki = get(zasvetkaIntervals);
        const currentVki = get(vkiIntervals);
        const currentRotations = get(rotationIntervals);
        
        programsList.update(list => 
            list.map(program => 
                program.id === currentActiveId 
                    ? {
                        ...program,
                        intervals: [...currentIntervals],
                        operatorData: currentOperator ? { ...currentOperator } : null,
                        ppiAssignments: [...currentPpi],
                        createdPrograms: [...currentCreated],
                        shadowIntervals: [...currentShadows],
                        zasvetkaIntervals: [...currentZasvetki],
                        vkiIntervals: [...currentVki],
                        rotationIntervals: [...currentRotations]
                    }
                    : program
            )
        );
    }

    // Удаление ПРЦА из анализа
    function deleteProgramFromAnalysis(programId: string) {
        programsList.update(list => {
            const newList = list.filter(p => p.id !== programId);
            
            // Если удаляем активную ПРЦА
            if (get(activeProgramId) === programId) {
                if (newList.length > 0) {
                    // Выбираем первую из оставшихся
                    selectProgram(newList[0].id);
                } else {
                    // Если список пуст - выходим из режима анализа
                    isAnalysisMode.set(false);
                    activeProgramId.set(null);
                }
            }
            
            return newList;
        });
    }

    // Выход из режима анализа
    function exitAnalysisMode() {
        isAnalysisMode.set(false);
        activeProgramId.set(null);
        programsList.set([]);
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

    // Форматирование tooltip для интервала
    function getIntervalTooltip(interval: TimeInterval): string {
        console.log(`🔍 getIntervalTooltip для интервала:`, {
            id: interval.id,
            mode: interval.mode,
            startTime: interval.startTime,
            dlit: interval.dlit
        });
        
        const programData = get(createdPrograms).find(p => p.timeInterval.id === interval.id);
        
        if (programData) {
            console.log(`  ✅ Найден programData:`, {
                tempId: programData.tempId,
                modeData: programData.modeData,
                tsData: programData.modeData.tsData
            });
            return TooltipFormatter.formatTooltip(interval, programData.modeData);
        }
        
        console.log(`  ❌ programData НЕ НАЙДЕН для interval.id:`, interval.id);
        console.log(`  Доступные createdPrograms:`, get(createdPrograms).map(p => ({
            tempId: p.tempId,
            intervalId: p.timeInterval.id
        })));
        
        return interval.title || '';
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
        getIntervalTitle,
        getIntervalTooltip,
        saveCurrentProgramToAnalysis,
        openAnalysisModal,
        closeAnalysisModal,
        createAnalysis,
        selectProgram,
        exitAnalysisMode,
        deleteProgramFromAnalysis,
        syncCurrentProgramWithStore 
    };
}
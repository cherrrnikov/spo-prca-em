import { DEFAULT_NUM_KA, MODE_CODES, WORK_MODES } from '$lib/constants/schedule';
import { MsuMapper } from '$lib/mappers/msuMapper';
import { modal } from '$lib/services/modal.service';
import type { CreatedProgramData, ProgramModeData, RotationInterval, ShadowInterval, TimeInterval, VkiInterval, ZasvetkaInterval } from '$lib/types';
import type { ProgramsListItem } from '$lib/types/analysis';
import { AstrocorrectionService } from '$lib/utils/astrocorrection';
import { checkAllConflicts } from '$lib/utils/interval';
import { TimeUtils } from '$lib/utils/time';
import { get } from 'svelte/store';
import { ScheduleApiService } from '../../../../features/services/scheduleApi.service';
import { ScheduleConverterService } from '../../../../features/services/scheduleConverter.service';
import { ScheduleCreationService } from '../../../../features/services/scheduleCreation.service';
import type { createStores } from '../stores';
import type { createValidation } from '../validation';

export function createAnalysisActions(
    stores: ReturnType<typeof createStores>,
    validation: ReturnType<typeof createValidation>
) {
    const {
        bortData,
        intervals,
        operatorData,
        ppiAssignments,
        createdPrograms,
        shadowIntervals,
        zasvetkaIntervals,
        vkiIntervals,
        rotationIntervals,
        programsList,
        activeProgramId,
        isAnalysisMode,
        analysisModal,
        selectedProgramDate,
        contextDate,
        creationMode,
        operatorDataLoaded,
        selectedMode,
        editingInterval,
        selectedIntervalId,
        numKa
    } = stores;

    const { syncCurrentProgramWithStore } = validation;

    // Управление модалкой
    function openAnalysisModal(date?: string) {
        const currentDate = date || get(selectedProgramDate) || get(contextDate);
        analysisModal.set({
            isOpen: true,
            startDate: currentDate,
            endDate: currentDate,
            isLoading: false
        });
    }

    function closeAnalysisModal() {
        analysisModal.update(modal => ({ ...modal, isOpen: false }));
    }

    // Создание анализа
    async function createAnalysis(startDate: string, endDate: string) {
        analysisModal.update(modal => ({ ...modal, isLoading: true }));
        
        try {
            // 1. Сохраняем текущую ПРЦА как исходную
            const sourceProgram = createSourceProgram();
            
            // 2. Добавляем исходную ПРЦА в список и входим в режим анализа
            programsList.update(list => [...list, sourceProgram]);
            activeProgramId.set(sourceProgram.id);
            isAnalysisMode.set(true);
            
            // 3. Генерируем остальные ПРЦА для выбранного диапазона
            const newPrograms = await generateProgramsForRange(sourceProgram, startDate, endDate);
            
            // 4. Добавляем все созданные ПРЦА в список
            programsList.update(list => [...list, ...newPrograms]);
            
            modal.alert("Успех", `Создано ${newPrograms.length} ПРЦА для анализа`, 'success');
            
        } catch (error) {
            console.error("Ошибка при создании анализа:", error);
            resetAnalysisState();
        } finally {
            analysisModal.update(modal => ({ ...modal, isLoading: false, isOpen: false }));
        }
    }

    // Создание объекта исходной ПРЦА из текущих сторов
    function createSourceProgram(): ProgramsListItem {
        const currentOperatorData = get(operatorData);
        const currentBortData = get(bortData);
        const currentNumKa = get(numKa);

        return {
            id: `program_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
            name: `ПРЦА ${TimeUtils.formatDate(get(selectedProgramDate))}`,
            date: get(selectedProgramDate),
            intervals: [...get(intervals)],
            operatorData: currentOperatorData ? JSON.parse(JSON.stringify(currentOperatorData)) : null,
            bortData: currentBortData ? JSON.parse(JSON.stringify(currentBortData)) : null,
            ppiAssignments: [...get(ppiAssignments)],
            createdPrograms: [...get(createdPrograms)],
            shadowIntervals: [...get(shadowIntervals)],
            zasvetkaIntervals: [...get(zasvetkaIntervals)],
            vkiIntervals: [...get(vkiIntervals)],
            rotationIntervals: [...get(rotationIntervals)],
            numKa: currentNumKa ?? DEFAULT_NUM_KA,
            numRp: undefined
        };
    }

    // Генерация ПРЦА для всего диапазона дат
    async function generateProgramsForRange(
        sourceProgram: ProgramsListItem,
        startDate: string,
        endDate: string
    ): Promise<ProgramsListItem[]> {
        const dates = TimeUtils.generateDateRange(startDate, endDate);
        const newPrograms: ProgramsListItem[] = [];
        
        for (const date of dates) {
            if (date === sourceProgram.date) continue;
            
            const program = await generateProgramForDate(date, sourceProgram);
            if (program) newPrograms.push(program);
        }
        
        return newPrograms;
    }

    // Генерация одной ПРЦА для конкретной даты
    async function generateProgramForDate(
        date: string,
        sourceProgram: ProgramsListItem
    ): Promise<ProgramsListItem | null> {
        // Загружаем данные для даты
        const { operatorDataForDate, bortDataForDate, forecastDataForDate, vkiDataForDate, rotationDataForDate } = 
            await loadDataForDate(date);
        
        // Конвертируем прогнозные данные
        const { shadowsForDate, zasvetkiForDate } = convertForecast(forecastDataForDate);
        
        // Конвертируем астрособытия
        const { vkiForDate, rotationsForDate } = convertAstroEvents(vkiDataForDate, rotationDataForDate, date);
        
        // Создаём интервалы и createdPrograms для даты
        const { intervalsForDate, createdProgramsForDate } = await createProgramsForDate(
            date,
            sourceProgram,
            operatorDataForDate
        );
        
        // Добавляем астрокоррекции
        const isFullAstroMode = determineAstroMode(vkiDataForDate, rotationDataForDate, date);
        const intervalsWithAstro = AstrocorrectionService.mergeAstrocorrection(
            intervalsForDate,
            date,
            isFullAstroMode
        );
        
        // Проверяем конфликты
        const intervalsWithConflicts = checkAllConflicts(
            intervalsWithAstro,
            zasvetkiForDate,
            shadowsForDate,
            vkiForDate,
            rotationsForDate
        );
        
        return createProgramsListItem(
            date,
            intervalsWithConflicts,
            operatorDataForDate,
            bortDataForDate,
            sourceProgram.ppiAssignments,
            createdProgramsForDate,
            shadowsForDate,
            zasvetkiForDate,
            vkiForDate,
            rotationsForDate,
            sourceProgram.numKa ?? DEFAULT_NUM_KA,
            sourceProgram.numRp
        );
    }

    // Сброс состояния при ошибке
    function resetAnalysisState() {
        isAnalysisMode.set(false);
        activeProgramId.set(null);
        programsList.set([]);
    }

    // Управление ПРЦА в анализе
    function selectProgram(programId: string) {
        syncCurrentProgramWithStore();

        const program = get(programsList).find(p => p.id === programId);
        if (!program) return;
        
        intervals.set(program.intervals);
        operatorData.set(program.operatorData);
        ppiAssignments.set(program.ppiAssignments);
        createdPrograms.set(program.createdPrograms);
        shadowIntervals.set(program.shadowIntervals);
        zasvetkaIntervals.set(program.zasvetkaIntervals);
        vkiIntervals.set(program.vkiIntervals);
        rotationIntervals.set(program.rotationIntervals);
        contextDate.set(program.date);
        selectedProgramDate.set(program.date);
        activeProgramId.set(programId);
    }

    function deleteProgramFromAnalysis(programId: string) {
        programsList.update(list => {
            const newList = list.filter(p => p.id !== programId);
            
            // СЛУЧАЙ 1: Если после удаления не осталось ПРЦА
            if (newList.length === 0) {
                resetToInitialState();
                return [];
            }
            
            // СЛУЧАЙ 2: Если после удаления осталась ровно одна ПРЦА
            if (newList.length === 1) {
                const lastProgram = newList[0];
                
                // Делаем эту ПРЦА активной и выходим из анализа
                intervals.set(lastProgram.intervals);
                operatorData.set(lastProgram.operatorData);
                ppiAssignments.set(lastProgram.ppiAssignments);
                createdPrograms.set(lastProgram.createdPrograms);
                shadowIntervals.set(lastProgram.shadowIntervals);
                zasvetkaIntervals.set(lastProgram.zasvetkaIntervals);
                vkiIntervals.set(lastProgram.vkiIntervals);
                rotationIntervals.set(lastProgram.rotationIntervals);
                contextDate.set(lastProgram.date);
                selectedProgramDate.set(lastProgram.date);
                
                isAnalysisMode.set(false);
                activeProgramId.set(null);

                return [];
            }
            
            // СЛУЧАЙ 3: Если удаляем активную ПРЦА и осталось несколько
            if (get(activeProgramId) === programId) {
                // Переключаемся на первую в списке
                selectProgram(newList[0].id);
            }
            
            return newList;
        });
    }

    function exitAnalysisMode() {
        resetToInitialState();
    }

    function resetToInitialState() {
        // Сбрасываем режим анализа
        isAnalysisMode.set(false);
        activeProgramId.set(null);
        programsList.set([]);
        
        // Очищаем все данные ПРЦА
        intervals.set([]);
        operatorData.set(null);
        ppiAssignments.set([]);
        createdPrograms.set([]);
        shadowIntervals.set([]);
        zasvetkaIntervals.set([]);
        vkiIntervals.set([]);
        rotationIntervals.set([]);
        
        // Сбрасываем режим создания
        creationMode.set(null);
        operatorDataLoaded.set(false);
        
        // Сбрасываем редактирование
        selectedMode.set(null);
        editingInterval.set(null);
        selectedIntervalId.set(null);
        
        // Сбрасываем дату (чтобы заголовок исчез)
        selectedProgramDate.set('');
        contextDate.set('');
    }

    // Вспомогательные функции
    async function loadDataForDate(date: string) {
        const [operator, bort, forecast, vki, rotation] = await Promise.allSettled([
            ScheduleApiService.loadOperatorData(date).catch(() => null),
            ScheduleApiService.loadBortData(date).catch(() => null),
            ScheduleApiService.loadForecastData(date).catch(() => null),
            ScheduleApiService.loadVkiData(date).catch(() => null),
            ScheduleApiService.loadRotationData(date).catch(() => null)
        ]);
        
        return {
            operatorDataForDate: operator.status === 'fulfilled' ? operator.value : null,
            bortDataForDate: bort.status === 'fulfilled' ? bort.value : null,
            forecastDataForDate: forecast.status === 'fulfilled' ? forecast.value : null,
            vkiDataForDate: vki.status === 'fulfilled' ? vki.value : null,
            rotationDataForDate: rotation.status === 'fulfilled' ? rotation.value : null
        };
    }

    function convertForecast(forecastData: any) {
        if (!forecastData) return { shadowsForDate: [], zasvetkiForDate: [] };
        const forecast = ScheduleConverterService.convertForecastToIntervals(forecastData);
        return {
            shadowsForDate: forecast.shadows,
            zasvetkiForDate: forecast.zasvetki
        };
    }

    function convertAstroEvents(vkiData: any, rotationData: any, date: string) {
        return {
            vkiForDate: vkiData ? ScheduleConverterService.convertVkiToIntervals(vkiData) : [],
            rotationsForDate: rotationData ? ScheduleConverterService.convertRotationToIntervals(rotationData, date) : []
        };
    }

    function determineAstroMode(vkiData: any, rotationData: any, date: string): boolean {
        if (vkiData?.impulses?.length > 0) return true;
        
        if (rotationData?.rotations) {
            for (const rotation of rotationData.rotations) {
                const rotationStart = rotation.data_n.split('T')[0];
                const rotationEnd = rotation.data_k ? rotation.data_k.split('T')[0] : rotationStart;
                if (date >= rotationStart && date <= rotationEnd) return true;
            }
        }
        return false;
    }

    // Основная логика создания интервалов и createdPrograms для одной даты
    async function createProgramsForDate(
        date: string,
        currentProgram: ProgramsListItem,
        operatorDataForDate: any
    ): Promise<{ intervalsForDate: TimeInterval[], createdProgramsForDate: CreatedProgramData[] }> {
        let intervalsForDate: TimeInterval[] = [];
        let createdProgramsForDate: CreatedProgramData[] = [];
        const currentBortData = get(bortData);

        if (operatorDataForDate) {
            
            const intervalsFromId06 = ScheduleCreationService.convertToTimeIntervals(
                operatorDataForDate,
                currentProgram.ppiAssignments,
                WORK_MODES,
                1,
                currentBortData
            );
            intervalsForDate = [...intervalsFromId06];
            
            // Создаём createdPrograms для интервалов из ИД06
            const mainId = operatorDataForDate.main?.id || 0;
            const numKa = operatorDataForDate.main?.n_ka || 1;
            
            // КВД
            if (operatorDataForDate.kvd_list) {
                operatorDataForDate.kvd_list.forEach((kvd: any) => {
                    const timeInterval = intervalsForDate.find(i => 
                        i.id.includes(`kvd_${kvd.id}`)
                    );
                    
                    if (timeInterval) {
                        const modeData: ProgramModeData = {
                            numRp: 0,
                            numKa: numKa,
                            dateOn: kvd.dn,
                            dateOff: kvd.dk,
                            kodMode: MODE_CODES.KVD,
                            numPpi: timeInterval.ppi || 1,
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
                        createdProgramsForDate.push({ tempId, modeData, timeInterval });
                    }
                });
            }
            
            // ТНП
            if (operatorDataForDate.tnp_list) {
                operatorDataForDate.tnp_list.forEach((tnp: any) => {
                    const timeInterval = intervalsForDate.find(i => 
                        i.id.includes(`tnp_${tnp.id}`)
                    );
                    
                    if (timeInterval) {
                        const modeData: ProgramModeData = {
                            numRp: 0,
                            numKa: numKa,
                            dateOn: tnp.dn,
                            dateOff: tnp.dk,
                            kodMode: MODE_CODES.TNP,
                            numPpi: timeInterval.ppi || 1,
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
                        createdProgramsForDate.push({ tempId, modeData, timeInterval });
                    }
                });
            }
            
            // ТС
            if (operatorDataForDate.ts_list) {
                for (const ts of operatorDataForDate.ts_list) {
                    const tsSubIntervals = intervalsForDate.filter(i => 
                        i.id.startsWith(`ts_${ts.id}`)
                    );
                    
                    tsSubIntervals.forEach((subInterval, idx) => {
                        const modeData: ProgramModeData = {
                            numRp: 0,
                            numKa: numKa,
                            dateOn: `${date}T${subInterval.startTime}`,
                            dateOff: `${date}T${subInterval.endTime}`,
                            kodMode: MODE_CODES.TS,
                            numPpi: subInterval.ppi || 1,
                            dlit: subInterval.dlit || 420,
                            msuData: MsuMapper.fromId06(ts, mainId, currentBortData, subInterval.dlit || 420)
                        };
                        
                        const tempId = `ts_${ts.id}_${idx}_${date.replace(/-/g, '')}`;
                        createdProgramsForDate.push({ tempId, modeData, timeInterval: subInterval });
                    });
                }
            }
            
            // ОНА
            if (operatorDataForDate.ona_list) {
                operatorDataForDate.ona_list.forEach((ona: any) => {
                    const timeInterval = intervalsForDate.find(i => 
                        i.id.includes(`ona_${ona.id}`)
                    );
                    
                    if (timeInterval) {
                        const modeData: ProgramModeData = {
                            numRp: 0,
                            numKa: numKa,
                            dateOn: ona.dn,
                            dateOff: ona.dk,
                            kodMode: MODE_CODES.ONA,
                            numPpi: timeInterval.ppi || 1,
                            dlit: ona.dlit,
                            onaData: {
                                id: ona.id,
                                idMain: ona.id_main,
                                typeOmi: ona.typeOmi,
                                dN: ona.dn,
                                dK: ona.dk,
                                nOna: ona.n_ona,
                                nPpi: timeInterval.ppi || 1
                            }
                        };
                        
                        const tempId = `ona_${ona.id}_${date.replace(/-/g, '')}`;
                        createdProgramsForDate.push({ tempId, modeData, timeInterval });
                    }
                });
            }
            
        } else {
            
            intervalsForDate = currentProgram.intervals.map(interval => ({
                ...interval,
                id: `${interval.id}_${date.replace(/-/g, '')}`,
                date: date
            }));
            
            createdProgramsForDate = currentProgram.createdPrograms.map(p => {
                return {
                    ...p,
                    tempId: `${p.tempId}_${date.replace(/-/g, '')}`,
                    modeData: {
                        ...p.modeData,
                        dateOn: p.modeData.dateOn.replace(p.timeInterval.date, date),
                        dateOff: p.modeData.dateOff.replace(p.timeInterval.date, date)
                    },
                    timeInterval: {
                        ...p.timeInterval,
                        id: `${p.timeInterval.id}_${date.replace(/-/g, '')}`,
                        date: date
                    }
                };
            });
        }
        
        return { intervalsForDate, createdProgramsForDate };
    }

    function createProgramsListItem(
        date: string,
        intervals: TimeInterval[],
        operatorData: any,
        bortData: any,
        ppiAssignments: any[],
        createdPrograms: CreatedProgramData[],
        shadows: ShadowInterval[],
        zasvetki: ZasvetkaInterval[],
        vki: VkiInterval[],
        rotations: RotationInterval[],
        numKa: number,
        numRp?: number
    ): ProgramsListItem {
        return {
            id: `program_${date.replace(/-/g, '')}_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
            name: `ПРЦА ${TimeUtils.formatDate(date)}`,
            date: date,
            intervals: intervals,
            operatorData: operatorData,
            bortData: bortData,
            ppiAssignments: [...ppiAssignments],
            createdPrograms: createdPrograms,
            shadowIntervals: shadows,
            zasvetkaIntervals: zasvetki,
            vkiIntervals: vki,
            rotationIntervals: rotations,
            numKa: numKa,
            numRp: numRp
        };
    }

    return {
        openAnalysisModal,
        closeAnalysisModal,
        createAnalysis,
        selectProgram,
        exitAnalysisMode,
        deleteProgramFromAnalysis
    };
}
import { WORK_MODES } from '$lib/constants/schedule';
import type { CreatedProgramData, ProgramModeData, RotationInterval, ShadowInterval, TimeInterval, VkiInterval, ZasvetkaInterval } from '$lib/types';
import type { ProgramsListItem } from '$lib/types/analysis';
import { AstrocorrectionService } from '$lib/utils/astrocorrection.service';
import { checkAllConflicts } from '$lib/utils/interval';
import { TimeUtils } from '$lib/utils/time';
import { get } from 'svelte/store';
import { ScheduleApiService } from '../../../../features/services/api/scheduleApi.service';
import { ScheduleConverterService } from '../../../../features/services/data/scheduleConverter.service';
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
        contextDate
    } = stores;

    const { syncCurrentProgramWithStore } = validation;

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
                if (date === currentProgram.date) continue;
                
                console.log(`\n--- Обработка даты: ${date} ---`);
                
                // Загружаем данные для даты
                const { operatorDataForDate, bortDataForDate, forecastDataForDate, vkiDataForDate, rotationDataForDate } = 
                    await loadDataForDate(date);
                
                // Конвертируем данные в интервалы
                const { shadowsForDate, zasvetkiForDate } = convertForecast(forecastDataForDate);
                const { vkiForDate, rotationsForDate } = convertAstroEvents(vkiDataForDate, rotationDataForDate, date);
                
                // Создаём интервалы и createdPrograms для даты
                const { intervalsForDate, createdProgramsForDate } = await createProgramsForDate(
                    date,
                    currentProgram,
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
                
                newPrograms.push(createProgramsListItem(
                    date,
                    intervalsWithConflicts,
                    operatorDataForDate,
                    bortDataForDate,
                    currentProgram.ppiAssignments,
                    createdProgramsForDate,
                    shadowsForDate,
                    zasvetkiForDate,
                    vkiForDate,
                    rotationsForDate
                ));
            }
            
            programsList.update(list => [...list, ...newPrograms]);
            console.log(`\n=== ИТОГО: создано ${newPrograms.length} ПРЦА для анализа ===`);
            alert(`Создано ${newPrograms.length} ПРЦА для анализа`);
            
        } catch (error) {
            console.error("Ошибка при создании анализа:", error);
        } finally {
            analysisModal.update(modal => ({ ...modal, isLoading: false, isOpen: false }));
        }
    }

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
        activeProgramId.set(programId);
    }

    function exitAnalysisMode() {
        isAnalysisMode.set(false);
        activeProgramId.set(null);
        programsList.set([]);
    }

    function deleteProgramFromAnalysis(programId: string) {
        programsList.update(list => {
            const newList = list.filter(p => p.id !== programId);
            
            if (get(activeProgramId) === programId) {
                if (newList.length > 0) {
                    selectProgram(newList[0].id);
                } else {
                    isAnalysisMode.set(false);
                    activeProgramId.set(null);
                }
            }
            
            return newList;
        });
    }

    // Вспомогательные функции для createAnalysis
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

    async function createProgramsForDate(
        date: string,
        currentProgram: ProgramsListItem,
        operatorDataForDate: any
    ): Promise<{ intervalsForDate: TimeInterval[], createdProgramsForDate: CreatedProgramData[] }> {
        let intervalsForDate: TimeInterval[] = [];
        let createdProgramsForDate: CreatedProgramData[] = [];
        
        if (operatorDataForDate) {
            console.log(`Есть данные ИД06 для ${date}, создаём интервалы из них`);
            
            const hasKvd = operatorDataForDate.kvd_list?.length > 0;
            const hasTnp = operatorDataForDate.tnp_list?.length > 0;
            const hasTs = operatorDataForDate.ts_list?.length > 0;
            const hasOna = operatorDataForDate.ona_list?.length > 0;
            
            console.log(`Типы в ИД06: КВД:${hasKvd}, ТНП:${hasTnp}, ТС:${hasTs}, ОНА:${hasOna}`);
            
            const intervalsFromId06 = ScheduleCreationService.convertToTimeIntervals(
                operatorDataForDate,
                currentProgram.ppiAssignments,
                WORK_MODES
            );
            
            intervalsForDate = [...intervalsFromId06];
            const existingIds = new Set(intervalsFromId06.map(i => i.id));
            
            // Добавляем недостающие типы из исходной ПРЦА
            currentProgram.intervals.forEach(interval => {
                if (interval.isAstrocorrection) return;
                
                let shouldCopy = false;
                let typeName = '';
                if (interval.mode === 7 && !hasKvd) {
                    shouldCopy = true;
                    typeName = 'КВД';
                } else if (interval.mode === 4 && !hasTnp) {
                    shouldCopy = true;
                    typeName = 'ТНП';
                } else if (interval.mode === 8 && !hasTs) {
                    shouldCopy = true;
                    typeName = 'ТС';
                } else if (interval.mode === 6 && !hasOna) {
                    shouldCopy = true;
                    typeName = 'ОНА';
                } else if (interval.mode === 1 && !hasTs) { // Обычные съемки (mode 1)
                    shouldCopy = true;
                    typeName = 'Съемка';
                } else if (interval.mode === 2 && !hasTnp) { // ОМИ (mode 2)
                    shouldCopy = true;
                    typeName = 'ОМИ';
                }
                
                if (shouldCopy) {
                    const newInterval = {
                        ...interval,
                        id: `${interval.id}_${date.replace(/-/g, '')}`,
                        date: date
                    };
                    
                    if (!existingIds.has(newInterval.id)) {
                        intervalsForDate.push(newInterval);
                        existingIds.add(newInterval.id);
                        
                        const originalProgram = currentProgram.createdPrograms.find(p => 
                            p.timeInterval.id === interval.id
                        );
                        
                        if (originalProgram) {
                            // 👇 ВАЖНО: обновляем даты в modeData для всех типов
                            const newModeData = { ...originalProgram.modeData };
                            
                            // Обновляем основные поля дат
                            newModeData.dateOn = originalProgram.modeData.dateOn.replace(
                                originalProgram.timeInterval.date, 
                                date
                            );
                            newModeData.dateOff = originalProgram.modeData.dateOff.replace(
                                originalProgram.timeInterval.date, 
                                date
                            );
                            
                            // Обновляем даты в специализированных полях если они есть
                            if (newModeData.onaData) {
                                newModeData.onaData.dN = newModeData.onaData.dN.replace(
                                    originalProgram.timeInterval.date, 
                                    date
                                );
                                newModeData.onaData.dK = newModeData.onaData.dK.replace(
                                    originalProgram.timeInterval.date, 
                                    date
                                );
                            }
                            
                            if (newModeData.omiData) {
                                newModeData.omiData.dateNach = newModeData.omiData.dateNach.replace(
                                    originalProgram.timeInterval.date, 
                                    date
                                );
                                newModeData.omiData.dateCon = newModeData.omiData.dateCon.replace(
                                    originalProgram.timeInterval.date, 
                                    date
                                );
                            }
                            
                            createdProgramsForDate.push({
                                ...originalProgram,
                                tempId: `${originalProgram.tempId}_${date.replace(/-/g, '')}`,
                                modeData: newModeData,
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
                        a.recordType === 'ts'
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
            
            // ТС
            if (operatorDataForDate.ts_list) {
                const tsAssignment = currentProgram.ppiAssignments.find(a => a.recordType === 'ts');

                for (const ts of operatorDataForDate.ts_list) {
                    if (tsAssignment) {
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
                                numPpi: tsAssignment.ppiNum,
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
            console.log(`  Исходная дата: ${currentProgram.date}, новая дата: ${date}`);
            
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
        rotations: RotationInterval[]
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
            rotationIntervals: rotations
        };
    }

    return {
        saveCurrentProgramToAnalysis,
        openAnalysisModal,
        closeAnalysisModal,
        createAnalysis,
        selectProgram,
        exitAnalysisMode,
        deleteProgramFromAnalysis
    };
}
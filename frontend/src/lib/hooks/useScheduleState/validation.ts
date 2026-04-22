import { MODE_CODES, WORK_MODES } from '$lib/constants/schedule';
import { ConstraintValidator } from '$lib/services/constraints/constraintValidator.service';
import type { TimeInterval } from '$lib/types';
import { checkTwoIntervalsOverlap } from '$lib/utils/interval/conflicts';
import { checkAllConflicts } from '$lib/utils/interval/index';
import { checkZasvetkaProximity } from '$lib/utils/interval/zasvetka';
import { get } from 'svelte/store';
import { ScheduleCreationService } from '../../../features/services/scheduleCreation.service';
import type { createStores } from './stores';

export function createValidation(stores: ReturnType<typeof createStores>) {
    const {
        intervals,
        operatorData,
        ppiAssignments,
        shadowIntervals,
        zasvetkaIntervals,
        vkiIntervals,
        rotationIntervals,
        programsList,
        activeProgramId,
        createdPrograms
    } = stores;

    function checkAndUpdateAllConflictsForNewInterval(newInterval: TimeInterval) {
        const currentIntervals = get(intervals);
        const currentOperatorData = get(operatorData);
        const currentPpiAssignments = get(ppiAssignments);
        const currentZasvetkaIntervals = get(zasvetkaIntervals);
        const currentVki = get(vkiIntervals);
        const currentRotations = get(rotationIntervals);
        const currentAstro = currentIntervals.filter(i => i.isAstrocorrection);

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
        
        // Проверка конфликтов с другими режимами
        let hasConflict = false;
        const conflictWith: number[] = [];
        
        for (const existingInterval of allIntervals) {
            if (existingInterval.mode === newInterval.mode) continue;

            // КВД может работать одновременно со съёмкой — не конфликт
            const isKvdWithShooting =
                (newInterval.mode === MODE_CODES.KVD && (existingInterval.mode === MODE_CODES.SHOOTING || existingInterval.mode === MODE_CODES.TS)) ||
                (existingInterval.mode === MODE_CODES.KVD && (newInterval.mode === MODE_CODES.SHOOTING || newInterval.mode === MODE_CODES.TS));

            if (isKvdWithShooting) continue;
            
            const overlap = checkTwoIntervalsOverlap(
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

        // Отдельная проверка КВД — конфликт только если нет совпадающей съёмки
        if (newInterval.mode === MODE_CODES.KVD) {
            const overlappingShots = allIntervals.filter(i =>
                (i.mode === MODE_CODES.SHOOTING || i.mode === MODE_CODES.TS) &&
                checkTwoIntervalsOverlap(newInterval.startTime, newInterval.endTime, i.startTime, i.endTime)
            );

            if (overlappingShots.length === 0) {
                hasConflict = true;
            } else {
                const hasMatch = overlappingShots.some(s => {
                    console.log('MATCH CHECK:', 
                        'kvdMsu:', newInterval.kvdConfig?.prMsu,
                        'shootMsu1:', s.msuData?.prMsu1,
                        'shootMsu2:', s.msuData?.prMsu2,
                        'kvdBssd:', newInterval.kvdConfig?.prBssd,
                        'shootBssd:', s.msuData?.prBssd
                    );
                    const startOk = s.startTime === newInterval.startTime;
                    const dlitOk  = (s.dlit ?? 0) === (newInterval.dlit ?? 0);

                    const kvdMsu  = newInterval.kvdConfig?.prMsu ?? 0;  // 0=МСУ1, 1=МСУ2
                    const kvdBssd = newInterval.kvdConfig?.prBssd ?? 0;

                    // kvdMsu=0 значит МСУ1 - у съёмки prMsu1 должен быть 1 (задействован)
                    // kvdMsu=1 значит МСУ2 - у съёмки prMsu2 должен быть 1
                    const msuOk  = kvdMsu === 0 ? (s.msuData?.prMsu1 === 1) : (s.msuData?.prMsu2 === 1);
                    const bssdOk = kvdBssd === (s.msuData?.prBssd ?? 0);

                    return startOk && dlitOk && msuOk && bssdOk;
                });
                hasConflict = !hasMatch;
            }
        }

        // Проверка ограничений
        const constraintViolations = ConstraintValidator.validate(
            [newInterval],
            currentVki,
            currentRotations,
            currentAstro,
            get(shadowIntervals),
            currentZasvetkaIntervals
        );

        const hasViolations = constraintViolations.has(newInterval.id);
        
        // Проверка засветок
        const zasvetkaCheck = checkZasvetkaProximity(
            newInterval.startTime,
            newInterval.endTime,
            currentZasvetkaIntervals
        );

        newInterval.hasConflict = hasConflict;
        newInterval.conflictWith = conflictWith;
        newInterval.nearZasvetka = zasvetkaCheck.nearZasvetka;
        newInterval.zasvetkaConflict = zasvetkaCheck.zasvetkaConflict;
        newInterval.zasvetkaDistance = zasvetkaCheck.minDistance;
        newInterval.constraintViolations = constraintViolations.get(newInterval.id) || [];
        
        // Финальный расчет willBeSaved
        newInterval.willBeSaved = 
            !hasConflict && 
            !zasvetkaCheck.zasvetkaConflict && 
            !zasvetkaCheck.nearZasvetka &&
            !hasViolations;
    }

    function updateAllConflicts() {
        const currentIntervals = get(intervals);
        const currentZasvetka = get(zasvetkaIntervals) || [];
        const currentShadows = get(shadowIntervals) || [];
        const currentVki = get(vkiIntervals) || [];
        const currentRotations = get(rotationIntervals) || [];
        
        const intervalsWithConflicts = checkAllConflicts(
            currentIntervals,
            currentZasvetka,
            currentShadows,
            currentVki,
            currentRotations 
        );
        
        intervals.set(
            currentIntervals.map(interval => {
                const updatedInterval = intervalsWithConflicts.find((i: { id: string; }) => i.id === interval.id);
                if (updatedInterval) {
                    return {
                        ...interval,
                        hasConflict: updatedInterval.hasConflict,
                        conflictWith: updatedInterval.conflictWith,
                        nearZasvetka: updatedInterval.nearZasvetka,
                        zasvetkaConflict: updatedInterval.zasvetkaConflict,
                        zasvetkaDistance: updatedInterval.zasvetkaDistance,
                        inShadow: updatedInterval.inShadow,
                        willBeSavedInShadow: updatedInterval.willBeSavedInShadow,
                        shadowPriority: updatedInterval.shadowPriority,
                        willBeSaved: updatedInterval.willBeSaved,
                        constraintViolations: updatedInterval.constraintViolations,
                        msu1Config: updatedInterval.msu1Config ?? interval.msu1Config,
                        msu2Config: updatedInterval.msu2Config ?? interval.msu2Config,
                        emptyMsu: updatedInterval.emptyMsu ?? false,
                        msuData: updatedInterval.msuData ?? interval.msuData,
                    };
                }
                return interval;
            })
        );

        const currentCreatedPrograms = get(createdPrograms);
        const updatedCreatedPrograms = currentCreatedPrograms.map(program => {
            const updatedInterval = intervalsWithConflicts.find(i => i.id === program.timeInterval.id);
            if (updatedInterval) {
                return {
                    ...program,
                    timeInterval: {
                        ...program.timeInterval,
                        willBeSaved: updatedInterval.willBeSaved,
                        hasConflict: updatedInterval.hasConflict,
                        constraintViolations: updatedInterval.constraintViolations,
                        inShadow: updatedInterval.inShadow,
                        willBeSavedInShadow: updatedInterval.willBeSavedInShadow,
                        nearZasvetka: updatedInterval.nearZasvetka,
                        zasvetkaConflict: updatedInterval.zasvetkaConflict
                    },
                    modeData: updatedInterval.msuData ? {
                        ...program.modeData,
                        msuData: updatedInterval.msuData
                    } : program.modeData
                };
            }
            return program;
        });
        createdPrograms.set(updatedCreatedPrograms);
    }

    function syncCurrentProgramWithStore() {
        const currentActiveId = get(activeProgramId);
        if (!currentActiveId) return;
        
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

    return {
        checkAndUpdateAllConflictsForNewInterval,
        updateAllConflicts,
        syncCurrentProgramWithStore
    };
}
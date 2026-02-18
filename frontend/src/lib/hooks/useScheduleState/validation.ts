import { WORK_MODES } from '$lib/constants/schedule';
import { ConstraintValidator } from '$lib/services/constraints/constraintValidator.service';
import type { TimeInterval } from '$lib/types';
import { IntervalUtils } from '$lib/utils/interval';
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
        rotationIntervals
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
        const zasvetkaCheck = IntervalUtils.checkZasvetkaProximity(
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

    return {
        checkAndUpdateAllConflictsForNewInterval,
        updateAllConflicts
    };
}
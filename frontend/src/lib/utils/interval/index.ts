export { checkIntervalOverlap, checkTwoIntervalsOverlap } from './conflicts';
export { getDefaultIntervalFlags } from './defaults';
export { checkShadowPriority } from './shadow';
export { checkZasvetkaProximity } from './zasvetka';

import { MODE_CODES } from '$lib/constants/schedule';
import { ConstraintValidator } from '$lib/services/constraints/constraintValidator.service';
import type {
    RotationInterval,
    ShadowInterval,
    TimeInterval,
    VkiInterval,
    ZasvetkaInterval
} from '$lib/types';
import { checkTwoIntervalsOverlap } from './conflicts';
import { getDefaultIntervalFlags } from './defaults';
import { checkShadowPriority } from './shadow';
import { checkZasvetkaProximity } from './zasvetka';

export function checkAllConflicts(
    intervals: TimeInterval[],
    zasvetkaIntervals: ZasvetkaInterval[] = [],
    shadowIntervals: ShadowInterval[] = [],
    vkiIntervals: VkiInterval[] = [],
    rotationIntervals: RotationInterval[] = []
): TimeInterval[] {
    const astroIntervals = intervals.filter(i => i.isAstrocorrection);
    const regularIntervals = intervals.filter(i => !i.isAstrocorrection);
    
    const shadowProcessedIntervals = checkShadowPriority(regularIntervals, shadowIntervals || []);

    const constraintViolations = ConstraintValidator.validate(
        shadowProcessedIntervals,
        vkiIntervals,
        rotationIntervals,
        astroIntervals,
        shadowIntervals,
        zasvetkaIntervals
    );

    const withConstraints = shadowProcessedIntervals.map(interval => {
        const violations = constraintViolations.get(interval.id);
        return {
            ...interval,
            constraintViolations: violations || [],
            ...getDefaultIntervalFlags()
        };
    });
    
    // Проверка пересечений между режимами
    for (let i = 0; i < withConstraints.length; i++) {
        for (let j = i + 1; j < withConstraints.length; j++) {
            const intervalA = withConstraints[i];
            const intervalB = withConstraints[j];
            
            if (intervalA.mode === intervalB.mode) continue;

            // КВД может работать одновременно со съёмкой — это не конфликт
            const isKvdWithShooting = (
                (intervalA.mode === MODE_CODES.KVD && (intervalB.mode === MODE_CODES.SHOOTING || intervalB.mode === MODE_CODES.TS)) ||
                (intervalB.mode === MODE_CODES.KVD && (intervalA.mode === MODE_CODES.SHOOTING || intervalA.mode === MODE_CODES.TS))
            );

            if (isKvdWithShooting) {
                // КВД красный если время начала или длительность не совпадают со съёмкой
                const kvd    = intervalA.mode === MODE_CODES.KVD ? intervalA : intervalB;
                const shoot  = intervalA.mode === MODE_CODES.KVD ? intervalB : intervalA;
                
                const startMismatch = kvd.startTime !== shoot.startTime;
                const dlitMismatch  = (kvd.dlit ?? 0) !== (shoot.dlit ?? 0);
                
                if (startMismatch || dlitMismatch) {
                    kvd.hasConflict = true;
                }
                continue; // съёмку не трогаем
            }
            
            const overlap = checkTwoIntervalsOverlap(
                intervalA.startTime,
                intervalA.endTime,
                intervalB.startTime,
                intervalB.endTime
            );
            
            if (overlap) {
                intervalA.hasConflict = true;
                intervalB.hasConflict = true;
                
                if (!intervalA.conflictWith?.includes(intervalB.mode)) {
                    intervalA.conflictWith = [...(intervalA.conflictWith || []), intervalB.mode];
                }
                if (!intervalB.conflictWith?.includes(intervalA.mode)) {
                    intervalB.conflictWith = [...(intervalB.conflictWith || []), intervalA.mode];
                }
            }
        }
    }
    
    // Проверка засветок
    const zasvetkaArray = zasvetkaIntervals || [];
    withConstraints.forEach(interval => {
        const zasvetkaCheck = checkZasvetkaProximity(
            interval.startTime,
            interval.endTime,
            zasvetkaArray
        );
        
        interval.nearZasvetka = zasvetkaCheck.nearZasvetka;
        interval.zasvetkaConflict = zasvetkaCheck.zasvetkaConflict;
        interval.zasvetkaDistance = zasvetkaCheck.minDistance;
    });

    // Финальный расчет willBeSaved
    withConstraints.forEach(interval => {
        if (interval.inShadow) {
            interval.willBeSaved = interval.willBeSavedInShadow || false;
            if (!interval.willBeSaved) {
            }
        } else {
            interval.willBeSaved = true;
            
            let reason = null;
            if (interval.hasConflict) {
                interval.willBeSaved = false;
                reason = 'hasConflict';
            } else if (interval.zasvetkaConflict) {
                interval.willBeSaved = false;
                reason = 'zasvetkaConflict';
            } else if (interval.nearZasvetka) {
                interval.willBeSaved = false;
                reason = 'nearZasvetka';
            } else if (interval.constraintViolations && interval.constraintViolations.length > 0) {
                interval.willBeSaved = false;
                reason = `constraintViolations: ${interval.constraintViolations.map(v => v.constraintId).join(',')}`;
            }
            
            if (!interval.willBeSaved) {
            } else {
            }
        }
    });

    return [...withConstraints, ...astroIntervals];
}
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

            if (isKvdWithShooting) continue;
            
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

    // Отдельная проверка КВД — красный только если нет совпадающей съёмки
    withConstraints
        .filter(i => i.mode === MODE_CODES.KVD)
        .forEach(kvd => {
            const overlappingShots = withConstraints.filter(i =>
                (i.mode === MODE_CODES.SHOOTING || i.mode === MODE_CODES.TS) &&
                checkTwoIntervalsOverlap(kvd.startTime, kvd.endTime, i.startTime, i.endTime)
            );

            if (overlappingShots.length === 0) {
                // КВД без съёмки — конфликт
                kvd.hasConflict = true;
            } else {
                // Есть ли хотя бы одна съёмка с совпадающим временем и длительностью
                const hasMatch = overlappingShots.some(s => {
                    const startOk = s.startTime === kvd.startTime;  // в index.ts — kvd, в validation.ts — newInterval
                    const dlitOk  = (s.dlit ?? 0) === (kvd.dlit ?? 0);
                    
                    // Сравниваем признаки МСУ и БССД
                    const kvdMsu  = kvd.kvdConfig?.prMsu ?? 0;  // 0=МСУ1, 1=МСУ2
                    const kvdBssd = kvd.kvdConfig?.prBssd ?? 0;

                    // kvdMsu=0 значит МСУ1 - у съёмки prMsu1 должен быть 1 (задействован)
                    // kvdMsu=1 значит МСУ2 - у съёмки prMsu2 должен быть 1
                    const msuOk  = kvdMsu === 0 ? (s.msuData?.prMsu1 === 1) : (s.msuData?.prMsu2 === 1);
                    const bssdOk = kvdBssd === (s.msuData?.prBssd ?? 0);

                    return startOk && dlitOk && msuOk && bssdOk;
                });
                kvd.hasConflict = !hasMatch;
            }
        });
    
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
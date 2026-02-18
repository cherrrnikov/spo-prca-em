import type { ZasvetkaInterval } from '$lib/types';
import { TimeUtils } from '../time';
import { checkTwoIntervalsOverlap } from './conflicts';

export function checkZasvetkaProximity(
    intervalStart: string,
    intervalEnd: string,
    zasvetkaIntervals: ZasvetkaInterval[] = []
): {
    nearZasvetka: boolean;
    zasvetkaConflict: boolean;
    minDistance: number;
} {
    const intervalStartSeconds = TimeUtils.timeToSeconds(intervalStart);
    const intervalEndSeconds = TimeUtils.timeToSeconds(intervalEnd);
    const SAFETY_BUFFER = 60;
    
    let minDistance = Infinity;
    let nearZasvetka = false;
    let zasvetkaConflict = false;

    for (const zasvetka of zasvetkaIntervals) {
        const zasvetkaStart = TimeUtils.timeToSeconds(zasvetka.startTime);
        const zasvetkaEnd = TimeUtils.timeToSeconds(zasvetka.endTime);
        
        const overlaps = checkTwoIntervalsOverlap(
            intervalStart,
            intervalEnd,
            zasvetka.startTime,
            zasvetka.endTime
        );
        
        if (overlaps) {
            zasvetkaConflict = true;
            minDistance = 0;
            break;
        }
        
        if (intervalEndSeconds <= zasvetkaStart) {
            const distance = zasvetkaStart - intervalEndSeconds;
            if (distance < SAFETY_BUFFER) {
                nearZasvetka = true;
                minDistance = Math.min(minDistance, distance);
            }
        }
        
        if (intervalStartSeconds >= zasvetkaEnd) {
            const distance = intervalStartSeconds - zasvetkaEnd;
            if (distance < SAFETY_BUFFER) {
                nearZasvetka = true;
                minDistance = Math.min(minDistance, distance);
            }
        }
    }
    
    return {
        nearZasvetka,
        zasvetkaConflict,
        minDistance: minDistance === Infinity ? 0 : minDistance
    };
}
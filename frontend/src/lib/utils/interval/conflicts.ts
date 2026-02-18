import type { TimeInterval } from '$lib/types';
import { TimeUtils } from '../time';

export function checkTwoIntervalsOverlap(
    startA: string,
    endA: string,
    startB: string,
    endB: string
): boolean {
    const startSecondsA = TimeUtils.timeToSeconds(startA);
    const endSecondsA = TimeUtils.timeToSeconds(endA);
    const startSecondsB = TimeUtils.timeToSeconds(startB);
    const endSecondsB = TimeUtils.timeToSeconds(endB);
    
    return (
        (startSecondsA >= startSecondsB && startSecondsA < endSecondsB) ||
        (endSecondsA > startSecondsB && endSecondsA <= endSecondsB) ||
        (startSecondsA <= startSecondsB && endSecondsA >= endSecondsB)
    );
}

export function checkIntervalOverlap(
    intervals: TimeInterval[],
    newStartTime: string,
    newDuration: number,
    modeId: number,
    excludeIntervalId?: string
): { overlaps: boolean; conflictingInterval?: TimeInterval } {
    const newEndTime = TimeUtils.calculateEndTimeSeconds(newStartTime, newDuration);
    
    for (const interval of intervals) {
        if (excludeIntervalId && interval.id === excludeIntervalId) continue;
        if (interval.mode !== modeId) continue;
        if (interval.isAstrocorrection) continue;
        
        const overlaps = checkTwoIntervalsOverlap(
            newStartTime,
            newEndTime,
            interval.startTime,
            interval.endTime
        );
        
        if (overlaps) {
            return { overlaps: true, conflictingInterval: interval };
        }
    }
    
    return { overlaps: false };
}
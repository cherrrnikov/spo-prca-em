import type { ShadowInterval, TimeInterval } from '$lib/types';
import { TimeUtils } from '../time';

export function checkShadowPriority(
    intervals: TimeInterval[],
    shadowIntervals: ShadowInterval[] = []
): TimeInterval[] {
    if (!shadowIntervals || shadowIntervals.length === 0) {
        return intervals.map(interval => ({
            ...interval,
            inShadow: false,
            shadowPriority: 0,
            willBeSavedInShadow: false
        }));
    }

    const updatedIntervals = intervals.map(interval => ({
        ...interval,
        inShadow: false,
        shadowPriority: 0,
        willBeSavedInShadow: false
    }));

    for (const shadow of shadowIntervals) {
        const shadowStart = TimeUtils.timeToSeconds(shadow.startTime);
        const shadowEnd = TimeUtils.timeToSeconds(shadow.endTime);
        const shadowCenter = shadowStart + (shadowEnd - shadowStart) / 2;

        const intervalsInThisShadow = updatedIntervals.filter(interval => {
            const intervalStart = TimeUtils.timeToSeconds(interval.startTime);
            const intervalEnd = TimeUtils.timeToSeconds(interval.endTime);
            return intervalStart >= shadowStart && intervalEnd <= shadowEnd;
        });

        if (intervalsInThisShadow.length === 0) continue;

        intervalsInThisShadow.forEach(interval => {
            interval.inShadow = true;
            const intervalCenter = TimeUtils.timeToSeconds(interval.startTime) + 
                                 (TimeUtils.timeToSeconds(interval.endTime) - TimeUtils.timeToSeconds(interval.startTime)) / 2;
            interval.shadowPriority = Math.abs(intervalCenter - shadowCenter);
        });

        const sortedIntervals = [...intervalsInThisShadow].sort((a, b) => {
            if (a.shadowPriority !== b.shadowPriority) {
                return a.shadowPriority - b.shadowPriority;
            }
            return TimeUtils.timeToSeconds(a.startTime) - TimeUtils.timeToSeconds(b.startTime);
        });

        const winner = sortedIntervals[0];
        
        intervalsInThisShadow.forEach(interval => {
            interval.willBeSavedInShadow = false;
        });
        
        winner.willBeSavedInShadow = true;

        if (winner.mode === 8) {
            if (winner.msu1Config) {
                winner.msu1Config.prVdMsu = 0;
                winner.msu1Config.vd1 = 0;
                winner.msu1Config.vd2 = 0;
                winner.msu1Config.vd3 = 0;
            }
            
            if (winner.msu2Config) {
                winner.msu2Config.prVdMsu = 0;
                winner.msu2Config.vd2 = 0;
                winner.msu2Config.vd2 = 0;
                winner.msu2Config.vd3 = 0;
            }
        }

        intervalsInThisShadow
            .filter(i => i !== winner)
            .forEach(interval => {
                interval.willBeSaved = false;
            });
    }

    return updatedIntervals;
}
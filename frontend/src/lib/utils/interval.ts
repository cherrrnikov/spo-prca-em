import type { TimeInterval, ZasvetkaInterval } from '$lib/types/schedule';
import { IntervalValidationService } from './intervalValidation';
import { TimeUtils } from './time';

export class IntervalUtils {
    static checkTwoIntervalsOverlap(
        startA: string,
        endA: string,
        startB: string,
        endB: string
    ): boolean {
        const validationA = IntervalValidationService.validateInterval(startA, endA);
        const validationB = IntervalValidationService.validateInterval(startB, endB);
        
        if (!validationA.isValid || !validationB.isValid) {
            console.warn('Некорректный интервал в проверке конфликтов');
            return false; 
        }
        
        const startMinutesA = TimeUtils.timeToMinutes(startA);
        const endMinutesA = TimeUtils.timeToMinutes(endA);
        const startMinutesB = TimeUtils.timeToMinutes(startB);
        const endMinutesB = TimeUtils.timeToMinutes(endB);
        
        return (
            (startMinutesA >= startMinutesB && startMinutesA < endMinutesB) ||
            (endMinutesA > startMinutesB && endMinutesA <= endMinutesB) ||
            (startMinutesA <= startMinutesB && endMinutesA >= endMinutesB)
        );
    }

    static checkIntervalOverlap(
        intervals: TimeInterval[],
        newStartTime: string,
        newDuration: number,
        modeId: number,
        excludeIntervalId?: string
    ): { overlaps: boolean; conflictingInterval?: TimeInterval } {
        const newEndTime = TimeUtils.calculateEndTime(newStartTime, newDuration);
        
        for (const interval of intervals) {
            if (excludeIntervalId && interval.id === excludeIntervalId) {
                continue;
            }
            
            if (interval.mode !== modeId) {
                continue;
            }
            
            const overlaps = this.checkTwoIntervalsOverlap(
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

    static checkZasvetkaProximity(
        intervalStart: string,
        intervalEnd: string,
        zasvetkaIntervals: ZasvetkaInterval[]
    ): {
        nearZasvetka: boolean;
        zasvetkaConflict: boolean;
        minDistance: number;
    } {
        const intervalStartMinutes = TimeUtils.timeToMinutes(intervalStart);
        const intervalEndMinutes = TimeUtils.timeToMinutes(intervalEnd);
        const SAFETY_BUFFER = 1;
        
        let minDistance = Infinity;
        let nearZasvetka = false;
        let zasvetkaConflict = false;

        for (const zasvetka of zasvetkaIntervals) {
            const zasvetkaStart = TimeUtils.timeToMinutes(zasvetka.startTime);
            const zasvetkaEnd = TimeUtils.timeToMinutes(zasvetka.endTime);
            
            const overlaps = this.checkTwoIntervalsOverlap(
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
            
            if (intervalEndMinutes <= zasvetkaStart) {
                const distance = zasvetkaStart - intervalEndMinutes;
                if (distance < SAFETY_BUFFER) {
                    nearZasvetka = true;
                    minDistance = Math.min(minDistance, distance);
                }
            }
            
            if (intervalStartMinutes >= zasvetkaEnd) {
                const distance = intervalStartMinutes - zasvetkaEnd;
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

    static checkAllConflicts(
        intervals: TimeInterval[],
        zasvetkaIntervals: ZasvetkaInterval[]
    ): TimeInterval[] {
        const updatedIntervals = intervals.map(interval => ({
            ...interval,
            hasConflict: false,
            conflictWith: [],
            nearZasvetka: false,
            zasvetkaConflict: false,
            zasvetkaDistance: 0,
            willBeSaved: true
        }));
        
        for (let i = 0; i < updatedIntervals.length; i++) {
            for (let j = i + 1; j < updatedIntervals.length; j++) {
                const intervalA = updatedIntervals[i];
                const intervalB = updatedIntervals[j];
                
                if (intervalA.mode === intervalB.mode) {
                    continue;
                }
                
                const overlap = this.checkTwoIntervalsOverlap(
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
                    
                    intervalA.willBeSaved = false;
                    intervalB.willBeSaved = false;
                }
            }
        }
        
        updatedIntervals.forEach(interval => {
            const zasvetkaCheck = this.checkZasvetkaProximity(
                interval.startTime,
                interval.endTime,
                zasvetkaIntervals
            );
            
            interval.nearZasvetka = zasvetkaCheck.nearZasvetka;
            interval.zasvetkaConflict = zasvetkaCheck.zasvetkaConflict;
            interval.zasvetkaDistance = zasvetkaCheck.minDistance;
            
            if (zasvetkaCheck.zasvetkaConflict || zasvetkaCheck.nearZasvetka) {
                interval.willBeSaved = false;
            }
        });
        
        return updatedIntervals;
    }
}
import type { ShadowInterval, TimeInterval, ZasvetkaInterval } from '$lib/types/schedule';
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

    static checkIntervalOverlap(
        intervals: TimeInterval[],
        newStartTime: string,
        newDuration: number,
        modeId: number,
        excludeIntervalId?: string
    ): { overlaps: boolean; conflictingInterval?: TimeInterval } {
        const newEndTime = TimeUtils.calculateEndTimeSeconds(newStartTime, newDuration);
        
        for (const interval of intervals) {
            if (excludeIntervalId && interval.id === excludeIntervalId) {
                continue;
            }
            
            if (interval.mode !== modeId) {
                continue;
            }
            
            if (interval.isAstrocorrection) {
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

    static checkShadowPriority(
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

        console.log('Обработка теней. Всего теней:', shadowIntervals.length);

        for (const shadow of shadowIntervals) {
            const shadowStart = TimeUtils.timeToSeconds(shadow.startTime);
            const shadowEnd = TimeUtils.timeToSeconds(shadow.endTime);
            const shadowCenter = shadowStart + (shadowEnd - shadowStart) / 2;

            console.log(`Тень: ${shadow.startTime}-${shadow.endTime}, центр: ${shadowCenter} минут`);

            const intervalsInThisShadow = updatedIntervals.filter(interval => {
                const intervalStart = TimeUtils.timeToSeconds(interval.startTime);
                const intervalEnd = TimeUtils.timeToSeconds(interval.endTime);
                
                return intervalStart >= shadowStart && intervalEnd <= shadowEnd;
            });

            if (intervalsInThisShadow.length === 0) {
                continue;
            }

            intervalsInThisShadow.forEach(interval => {
                interval.inShadow = true;
                
                const intervalCenter = TimeUtils.timeToSeconds(interval.startTime) + 
                                     (TimeUtils.timeToSeconds(interval.endTime) - TimeUtils.timeToSeconds(interval.startTime)) / 2;
                interval.shadowPriority = Math.abs(intervalCenter - shadowCenter);
            });

            // Находим интервал с наименьшим расстоянием до центра тени
            if (intervalsInThisShadow.length > 0) {
                const sortedIntervals = [...intervalsInThisShadow].sort((a, b) => 
                    a.shadowPriority - b.shadowPriority
                );

                const bestPriority = sortedIntervals[0].shadowPriority;
                const bestIntervals = sortedIntervals.filter(i => 
                    Math.abs(i.shadowPriority - bestPriority) < 0.1 // Небольшой допуск для равенства
                );

                bestIntervals.forEach(interval => {
                    interval.willBeSavedInShadow = true;
                });

                // Все остальные интервалы в этой тени не будут сохранены
                intervalsInThisShadow
                    .filter(i => !bestIntervals.includes(i))
                    .forEach(interval => {
                        interval.willBeSaved = false;
                    });
            }
        }

        return updatedIntervals;
    }

    static checkZasvetkaProximity(
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

    static checkAllConflicts(
        intervals: TimeInterval[],
        zasvetkaIntervals: ZasvetkaInterval[] = [],
        shadowIntervals: ShadowInterval[] = []
    ): TimeInterval[] {
        const astroIntervals = intervals.filter(i => i.isAstrocorrection);
        const regularIntervals = intervals.filter(i => !i.isAstrocorrection);
        
        const shadowProcessedIntervals = this.checkShadowPriority(regularIntervals, shadowIntervals || []);

        const updatedIntervals = shadowProcessedIntervals.map(interval => ({
            ...interval,
            hasConflict: false,
            conflictWith: [],
            hasAstroConflict: false,
            astroConflictWith: [],
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
            
            for (const astroInterval of astroIntervals) {
                const overlap = this.checkTwoIntervalsOverlap(
                    updatedIntervals[i].startTime,
                    updatedIntervals[i].endTime,
                    astroInterval.startTime,
                    astroInterval.endTime
                );
                
                if (overlap) {
                    updatedIntervals[i].hasAstroConflict = true;
                    
                    if (!updatedIntervals[i].astroConflictWith?.includes(astroInterval.mode)) {
                        updatedIntervals[i].astroConflictWith = [
                            ...(updatedIntervals[i].astroConflictWith || []), 
                            astroInterval.mode
                        ];
                    }
                    
                    console.log(`Конфликт с астрокоррекцией: интервал ${updatedIntervals[i].id}`, {
                        interval: `${updatedIntervals[i].startTime}-${updatedIntervals[i].endTime}`,
                        astro: `${astroInterval.startTime}-${astroInterval.endTime}`,
                        mode: updatedIntervals[i].mode
                    });
                }
            }
        }
        
        const zasvetkaArray = zasvetkaIntervals || [];
        updatedIntervals.forEach(interval => {
            const zasvetkaCheck = this.checkZasvetkaProximity(
                interval.startTime,
                interval.endTime,
                zasvetkaArray
            );
            
            interval.nearZasvetka = zasvetkaCheck.nearZasvetka;
            interval.zasvetkaConflict = zasvetkaCheck.zasvetkaConflict;
            interval.zasvetkaDistance = zasvetkaCheck.minDistance;
            
            if (interval.inShadow && interval.willBeSavedInShadow) {
                interval.willBeSaved = true;
            } 
            else if (interval.inShadow) {
                interval.willBeSaved = false;
            }
            else {
                if (
                    interval.hasConflict || 
                    interval.zasvetkaConflict || 
                    interval.nearZasvetka || 
                    interval.hasAstroConflict
                ) {
                    interval.willBeSaved = false;
                }
            }
        });

        return [...updatedIntervals, ...astroIntervals];
    }
}
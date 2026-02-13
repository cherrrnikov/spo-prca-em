import type { TimeInterval } from '$lib/types/schedule';

export class AstrocorrectionService {
    private static readonly ASTRO_MODE_ID = 9;
    private static readonly ASTRO_COLOR = '#1e40af'; 
    private static readonly ASTRO_DURATION = 18;
    
    private static readonly FULL_INTERVALS = [
        { start: '03:37:00', end: '03:55:00' },
        { start: '07:37:00', end: '07:55:00' },
        { start: '11:37:00', end: '11:55:00' },
        { start: '15:37:00', end: '15:55:00' },
        { start: '19:37:00', end: '19:55:00' },
        { start: '23:37:00', end: '23:55:00' }
    ];
    
    private static readonly NORMAL_INTERVALS = [
        { start: '11:37:00', end: '11:55:00' },
        { start: '23:37:00', end: '23:55:00' }
    ];

    static createAstrocorrectionIntervals(date: string, isFullMode: boolean): TimeInterval[] {
        const timeSlots = isFullMode ? this.FULL_INTERVALS : this.NORMAL_INTERVALS;
        
        return timeSlots.map((slot, index) => ({
            id: `astro-${date.replace(/-/g, '')}-${index + 1}`,
            startTime: slot.start,
            endTime: slot.end,
            mode: this.ASTRO_MODE_ID,
            date: date,
            title: 'Астрокоррекция',
            color: this.ASTRO_COLOR,
            ppi: 0,
            dlit: this.ASTRO_DURATION,
            duration: this.ASTRO_DURATION,
            customerCode: 0,
            
            hasConflict: false,
            conflictWith: [],
            nearZasvetka: false,
            zasvetkaConflict: false,
            zasvetkaDistance: 0,
            inShadow: false,
            willBeSavedInShadow: false,
            shadowPriority: 0,
            isAstrocorrection: true
        }));
    }

    static filterOutAstrocorrection(intervals: TimeInterval[]): TimeInterval[] {
        return intervals.filter(interval => !interval.isAstrocorrection);
    }

    static mergeAstrocorrection(
        existingIntervals: TimeInterval[], 
        date: string, 
        isFullMode: boolean
    ): TimeInterval[] {
        const filtered = this.filterOutAstrocorrection(existingIntervals);
        
        const astroIntervals = this.createAstrocorrectionIntervals(date, isFullMode);
        
        return [...filtered, ...astroIntervals];
    }

    static hasAstrocorrection(intervals: TimeInterval[]): boolean {
        return intervals.some(interval => interval.isAstrocorrection);
    }

    static getAstrocorrectionIntervals(intervals: TimeInterval[]): TimeInterval[] {
        return intervals.filter(interval => interval.isAstrocorrection);
    }
}
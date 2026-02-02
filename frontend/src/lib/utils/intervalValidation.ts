import { ScheduleConverterService } from '../../features/services/data/scheduleConverter.service';
import { TimeUtils } from './time';

export class IntervalValidationService {
    /**
     * Проверяет и корректирует интервал, чтобы он не выходил за границы суток
     */
    static validateAndFixInterval(
        intervalStartTime: string,
        intervalEndTime: string,
        baseDate: Date
    ): { 
        startTime: string; 
        endTime: string; 
        wasTruncated: boolean;
        originalEndTime?: string;
    } {
        const startDateTime = ScheduleConverterService.createDateWithTime(baseDate, intervalStartTime);
        const endDateTime = ScheduleConverterService.createDateWithTime(baseDate, intervalEndTime);
        
        // Если интервал выходит за границы суток (00:00 - 23:59)
        if (endDateTime.getDate() !== startDateTime.getDate()) {
            // Обрезаем до 23:59
            const truncatedEndDateTime = new Date(startDateTime);
            truncatedEndDateTime.setHours(23, 59, 0, 0);
            
            return {
                startTime: ScheduleConverterService.extractTimeOnly(intervalStartTime),
                endTime: '23:59',
                wasTruncated: true,
                originalEndTime: ScheduleConverterService.extractTimeOnly(intervalEndTime)
            };
        }
        
        return {
            startTime: ScheduleConverterService.extractTimeOnly(intervalStartTime),
            endTime: ScheduleConverterService.extractTimeOnly(intervalEndTime),
            wasTruncated: false
        };
    }

    static isTimeValid(timeStr: string): boolean {
        const timeRegex = /^([01]?[0-9]|2[0-3]):([0-5][0-9])$/;
        if (!timeRegex.test(timeStr)) return false;
        
        const [hours, minutes] = timeStr.split(':').map(Number);
        return hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59;
    }

    static isIntervalWithinDay(startTime: string, endTime: string): boolean {
        const startMinutes = TimeUtils.timeToMinutes(startTime);
        const endMinutes = TimeUtils.timeToMinutes(endTime);
        
        return endMinutes > startMinutes && endMinutes <= 24 * 60; // 24:00 = 1440 минут
    }

    static validateTimeInput(startTime: string, duration: number): { 
        isValid: boolean; 
        message?: string; 
        correctedEndTime?: string 
    } {
        if (!this.isTimeValid(startTime)) {
            return { 
                isValid: false, 
                message: 'Некорректное время начала. Используйте формат HH:MM (00:00-23:59)' 
            };
        }

        const startMinutes = TimeUtils.timeToMinutes(startTime);
        const endMinutes = startMinutes + Math.floor(duration / 60);
        
        if (endMinutes >= 24 * 60) { // 24:00 = 1440 минут
            const correctedMinutes = 24 * 60 - 1; // 23:59
            
            return {
                isValid: false,
                message: `Интервал выходит за пределы суток. Максимальное время окончания: 23:59`,
                correctedEndTime: TimeUtils.minutesToTime(correctedMinutes)
            };
        }
        
        return { isValid: true };
    }

    static clampTimeToDayEnd(timeStr: string): string {
        const minutes = TimeUtils.timeToMinutes(timeStr);
        if (minutes >= 24 * 60) {
            return '23:59';
        }
        return timeStr;
    }

    /**
     * Рассчитывает конечное время с учетом границ суток
     */
    static calculateEndTimeWithBoundaries(startTime: string, duration: number): string {
        const startMinutes = TimeUtils.timeToMinutes(startTime);
        const endMinutes = startMinutes + Math.floor(duration / 60);
        
        if (endMinutes >= 24 * 60) {
            return '23:59';
        }
        
        return TimeUtils.minutesToTime(endMinutes);
    }
}
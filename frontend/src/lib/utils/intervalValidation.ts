import { TimeUtils } from '$lib/utils/time';

export class IntervalValidationService {
    static validateInterval(startTime: string, endTime: string): {
        isValid: boolean;
        message?: string;
    } {
        if (!this.isTimeValid(startTime) || !this.isTimeValid(endTime)) {
            if (endTime === '24:00:00' || endTime === '24:00') {
                return {
                    isValid: false,
                    message: 'Интервал выходит за пределы суток. Используйте 00:00:00-23:59:59'
                };
            }
            
            return {
                isValid: false,
                message: 'Некорректный формат времени. Используйте HH:MM:SS'
            };
        }

        const startSeconds = TimeUtils.timeToSeconds(startTime);
        const endSeconds = TimeUtils.timeToSeconds(endTime);
        
        if (endSeconds <= startSeconds) {
            return {
                isValid: false,
                message: 'Время окончания должно быть позже времени начала'
            };
        }
        
        if (endSeconds > 24 * 3600) {
            return {
                isValid: false,
                message: 'Интервал выходит за пределы суток. Используйте 00:00:00-23:59:59'
            };
        }
        
        return { isValid: true };
    }

    static validateTimeInput(startTime: string, duration: number): {
        isValid: boolean;
        message?: string;
    } {
        if (!this.isTimeValid(startTime)) {
            return {
                isValid: false,
                message: 'Некорректное время начала. Используйте HH:MM:SS'
            };
        }

        const endTime = TimeUtils.calculateEndTimeSeconds(startTime, duration);
        
        const startSeconds = TimeUtils.timeToSeconds(startTime);
        if (startSeconds + duration > 24 * 3600) {
            const remainingSeconds = 24 * 3600 - startSeconds;
            const remainingMinutes = Math.floor(remainingSeconds / 60);
            const remainingSecs = remainingSeconds % 60;
            
            return {
                isValid: false,
                message: `Интервал выходит за пределы суток. До конца суток ${remainingMinutes} мин ${remainingSecs} сек. Уменьшите длительность или выберите более раннее время.`
            };
        }
        
        return this.validateInterval(startTime, endTime);
    }

    static isTimeValid(timeStr: string): boolean {
        if (timeStr === '24:00:00' || timeStr === '24:00') {
            return false;
        }
        
        const timeRegex = /^([01]?[0-9]|2[0-3]):([0-5][0-9])(:([0-5][0-9]))?$/;
        if (!timeRegex.test(timeStr)) return false;
        
        const parts = timeStr.split(':');
        const hours = parseInt(parts[0]);
        const minutes = parseInt(parts[1]);
        const seconds = parts[2] ? parseInt(parts[2]) : 0;
        
        return hours >= 0 && hours <= 23 && 
               minutes >= 0 && minutes <= 59 && 
               seconds >= 0 && seconds <= 59;
    }
}
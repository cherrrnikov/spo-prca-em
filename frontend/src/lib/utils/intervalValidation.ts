import { TimeUtils } from '$lib/utils/time';

export class IntervalValidationService {
    static validateInterval(startTime: string, endTime: string): {
        isValid: boolean;
        message?: string;
    } {
        if (!TimeUtils.isTimeValid(startTime) || !TimeUtils.isTimeValid(endTime)) {
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
        if (!TimeUtils.isTimeValid(startTime)) {
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
}
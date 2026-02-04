import { TimeUtils } from './time';

export class IntervalValidationService {

    static validateInterval(startTime: string, endTime: string): {
        isValid: boolean;
        message?: string;
    } {
        if (!this.isTimeValid(startTime) || !this.isTimeValid(endTime)) {
            return {
                isValid: false,
                message: 'Некорректный формат времени. Используйте 00:00-23:59'
            };
        }

        const startMinutes = TimeUtils.timeToMinutes(startTime);
        const endMinutes = TimeUtils.timeToMinutes(endTime);
        
        if (endMinutes <= startMinutes) {
            return {
                isValid: false,
                message: 'Время окончания должно быть позже времени начала'
            };
        }
        
        if (endMinutes >= 24 * 60) {
            return {
                isValid: false,
                message: 'Интервал выходит за пределы суток. Используйте 00:00-23:59'
            };
        }
        
        return { isValid: true };
    }

    // Проверяет время начала + длительность
    static validateTimeInput(startTime: string, duration: number): {
        isValid: boolean;
        message?: string;
    } {
        if (!this.isTimeValid(startTime)) {
            return {
                isValid: false,
                message: 'Некорректное время начала. Используйте 00:00-23:59'
            };
        }

        const endTime = TimeUtils.calculateEndTime(startTime, duration);
        
        return this.validateInterval(startTime, endTime);
    }

    static isTimeValid(timeStr: string): boolean {
        const timeRegex = /^([01]?[0-9]|2[0-3]):([0-5][0-9])$/;
        if (!timeRegex.test(timeStr)) return false;
        
        const [hours, minutes] = timeStr.split(':').map(Number);
        return hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59;
    }
}
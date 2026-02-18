export class TimeUtils {
    // ========== БАЗОВЫЕ КОНВЕРТАЦИИ ==========
    
    static timeToSeconds(timeStr: string): number {
        try {
            const parts = timeStr.split(':');
            const hours = parseInt(parts[0]) || 0;
            const minutes = parseInt(parts[1]) || 0;
            const seconds = parts[2] ? parseFloat(parts[2]) : 0;
            
            return hours * 3600 + minutes * 60 + seconds;
        } catch {
            return 0;
        }
    }

    static secondsToTime(seconds: number): string {
        const hrs = Math.floor(seconds / 3600);
        const mins = Math.floor((seconds % 3600) / 60);
        const secs = Math.floor(seconds % 60);
        
        return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }

    // ========== РАСЧЕТЫ ==========

    static calculateEndTimeSeconds(startTime: string, durationSeconds: number): string {
        const startSeconds = this.timeToSeconds(startTime);
        const endSeconds = startSeconds + durationSeconds;
        return this.secondsToTime(endSeconds);
    }

    static calculateEndTime(startTime: string, durationMinutes: number): string {
        return this.calculateEndTimeSeconds(startTime, durationMinutes * 60);
    }

    static calculateDuration(startStr: string, endStr: string): number {
        const start = new Date(startStr);
        const end = new Date(endStr);
        return Math.floor((end.getTime() - start.getTime()) / 1000);
    }

    // ========== ФОРМАТИРОВАНИЕ ==========

    static formatDateTime(dateStr: string): string {
        try {
            const date = new Date(dateStr);
            return date.toLocaleString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });
        } catch {
            return dateStr;
        }
    }

    static formatDate(dateStr: string): string {
        try {
            const date = new Date(dateStr);
            return date.toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
            });
        } catch {
            return dateStr;
        }
    }

    static formatTimeOnly(dateStr: string): string {
        try {
            const date = new Date(dateStr);
            return date.toLocaleTimeString('ru-RU', {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });
        } catch {
            return dateStr;
        }
    }

    static formatTimeFromISO(isoString: string): string {
        try {
            const date = new Date(isoString);
            return date.toTimeString().substring(0, 5);
        } catch {
            return "00:00";
        }
    }

    // ========== ИЗВЛЕЧЕНИЕ ==========

    static extractTimeFromTimestamp(timestamp: string): string {
        try {
            const date = new Date(timestamp);
            return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`;
        } catch {
            return '00:00:00';
        }
    }

    static extractDateFromTimestamp(timestamp: string): string {
        try {
            return timestamp.split('T')[0];
        } catch {
            const today = new Date();
            return today.toISOString().split('T')[0];
        }
    }

    static extractTimeOnly(timeStr: string): string {
        try {
            if (timeStr.includes('T') || timeStr.includes('-')) {
                const date = new Date(timeStr);
                return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`;
            }
            
            const timeRegex = /^(\d{1,2}):(\d{2})(:(\d{2}))?$/;
            const match = timeStr.match(timeRegex);
            
            if (match) {
                const hours = parseInt(match[1]).toString().padStart(2, '0');
                const minutes = match[2];
                const seconds = match[4] ? match[4].padStart(2, '0') : '00';
                return `${hours}:${minutes}:${seconds}`;
            }
            
            return "00:00:00";
        } catch {
            return "00:00:00";
        }
    }

    // ========== ВАЛИДАЦИЯ ==========

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

    static isWithinDayBounds(timeStr: string): boolean {
        const seconds = this.timeToSeconds(timeStr);
        return seconds >= 0 && seconds < 24 * 3600;
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ ==========

    static createDateWithTime(baseDate: Date, timeStr: string): Date {
        const date = new Date(baseDate);
        
        if (timeStr.includes('T') || timeStr.includes('-')) {
            try {
                return new Date(timeStr);
            } catch {
            }
        }
        
        const [hours, minutes] = timeStr.split(':').map(Number);
        date.setHours(hours || 0, minutes || 0, 0, 0);
        return date;
    }
}
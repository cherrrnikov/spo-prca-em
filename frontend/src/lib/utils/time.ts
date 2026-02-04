export class TimeUtils {
    static timeToMinutes(timeStr: string): number {
        try {
            const [hours, minutes] = timeStr.split(':').map(Number);
            return hours * 60 + (minutes || 0);
        } catch {
            return 0;
        }
    }

    static minutesToTime(minutes: number): string {
        const hours = Math.floor(minutes / 60);
        const mins = minutes % 60;
        return `${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}`;
    }

    static calculateEndTime(startTime: string, duration: number): string {
        const startMinutes = this.timeToMinutes(startTime);
        const endMinutes = startMinutes + Math.floor(duration / 60);
        return this.minutesToTime(endMinutes);
    }

    static isWithinDayBounds(timeStr: string): boolean {
        const minutes = this.timeToMinutes(timeStr);
        return minutes >= 0 && minutes < 24 * 60;
    }

    static formatDate(dateString: string): string {
        const date = new Date(dateString);
        return date.toLocaleDateString('ru-RU', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    }

    static extractTimeFromTimestamp(timestamp: string): string {
        try {
            const date = new Date(timestamp);
            return date.getHours().toString().padStart(2, '0') + ':' + 
                   date.getMinutes().toString().padStart(2, '0');
        } catch {
            return '00:00';
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
}
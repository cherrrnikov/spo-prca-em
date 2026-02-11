export class TimeUtils {
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

    static calculateEndTimeSeconds(startTime: string, durationSeconds: number): string {
        const startSeconds = this.timeToSeconds(startTime);
        let endSeconds = startSeconds + durationSeconds;

        return this.secondsToTime(endSeconds);
    }

    static calculateEndTime(startTime: string, durationMinutes: number): string {
        return this.calculateEndTimeSeconds(startTime, durationMinutes * 60);
    }

    static isWithinDayBounds(timeStr: string): boolean {
        const seconds = this.timeToSeconds(timeStr);
        return seconds >= 0 && seconds < 24 * 3600;
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
}
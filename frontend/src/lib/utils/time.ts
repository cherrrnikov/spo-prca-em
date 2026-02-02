export class TimeUtils {
    static timeToMinutes(time: string): number {
        const [hours, minutes] = time.split(':').map(Number);
        return hours * 60 + (minutes || 0);
    }

    static minutesToTime(minutes: number): string {
        const hours = Math.floor(minutes / 60);
        const mins = minutes % 60;
        return `${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}`;
    }

    static calculateEndTime(startTime: string, duration: number): string {
        const [hours, minutes] = startTime.split(':').map(Number);
        const totalMinutes = hours * 60 + minutes + Math.floor(duration / 60);
        const endHours = Math.floor(totalMinutes / 60) % 24;
        const endMinutes = totalMinutes % 60;
        return `${endHours.toString().padStart(2, '0')}:${endMinutes.toString().padStart(2, '0')}`;
    }

    static formatDate(dateString: string): string {
        const date = new Date(dateString);
        return date.toLocaleDateString('ru-RU', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    }

    static calculateDateFromTime(timeString: string): string {
        const today = new Date();
        const [hours, minutes] = timeString.split(':').map(Number);
        today.setHours(hours, minutes, 0, 0);
        return today.toISOString();
    }

    static calculateEndDate(startTime: string, duration: number): string {
        const startDate = new Date(this.calculateDateFromTime(startTime));
        const endDate = new Date(startDate.getTime() + duration * 1000);
        return endDate.toISOString();
    }
}
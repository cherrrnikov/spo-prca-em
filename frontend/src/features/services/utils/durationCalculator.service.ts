export class DurationCalculatorService {
    static calculateDuration(startStr: string, endStr: string): number {
        const start = new Date(startStr);
        const end = new Date(endStr);
        return Math.floor((end.getTime() - start.getTime()) / 1000);
    }

    static calculateEndDate(
        baseDate: string,
        baseTime: string,
        additionalMinutes: number = 0
    ): string {
        const date = new Date(`${baseDate}T${baseTime}:00`);
        date.setMinutes(date.getMinutes() + additionalMinutes);
        return date.toISOString();
    }
}
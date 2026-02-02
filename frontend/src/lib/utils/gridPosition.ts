export class GridPositionUtils {
    static readonly MIN_CELL_WIDTH = 40;
    static readonly MAX_CELL_WIDTH = 69;
    static readonly ROW_HEIGHT = 40;
    static readonly TIME_HEIGHT = 40;
    static readonly HOURS = Array.from({length: 24}, (_, i) => i);

    static calculateCellWidth(containerWidth: number): number {
        const availableWidth = containerWidth;
        const calculatedWidth = availableWidth / 24;
        return Math.max(
            this.MIN_CELL_WIDTH, 
            Math.min(calculatedWidth, this.MAX_CELL_WIDTH)
        );
    }

    static timeToMinutes(time: string): number {
        const [hours, minutes] = time.split(':').map(Number);
        return hours * 60 + (minutes || 0);
    }

    static minutesToPixels(minutes: number, cellWidth: number): number {
        return (minutes / 60) * cellWidth;
    }

    static getPositionForInterval(
        startTime: string,
        endTime: string,
        modeIndex: number,
        cellWidth: number
    ) {
        const startMinutes = this.timeToMinutes(startTime);
        const endMinutes = this.timeToMinutes(endTime);
        const durationMinutes = endMinutes - startMinutes;

        return {
            left: `${this.minutesToPixels(startMinutes, cellWidth)}px`,
            width: `${this.minutesToPixels(durationMinutes, cellWidth)}px`,
            top: `${this.TIME_HEIGHT + modeIndex * this.ROW_HEIGHT - 15}px`,
            height: `${this.ROW_HEIGHT - 10}px`
        };
    }
}
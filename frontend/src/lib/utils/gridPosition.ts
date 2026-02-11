import { TimeUtils } from "./time";

export class GridPositionUtils {
    static readonly MIN_CELL_WIDTH = 40;
    static readonly MAX_CELL_WIDTH = 69;
    static readonly ROW_HEIGHT = 40;
    static readonly TIME_HEIGHT = 40;
    static readonly HOURS = Array.from({length: 24}, (_, i) => i);
    static readonly SECONDS_IN_DAY = 86400;

    static calculateCellWidth(containerWidth: number): number {
        const availableWidth = containerWidth;
        const calculatedWidth = availableWidth / 24;
        return Math.max(
            this.MIN_CELL_WIDTH, 
            Math.min(calculatedWidth, this.MAX_CELL_WIDTH)
        );
    }

    static timeToSeconds(time: string): number {
        return TimeUtils.timeToSeconds(time);
    }

    static secondsToPixels(seconds: number, cellWidth: number): number {
        return (seconds / 3600) * cellWidth;
    }

    static getPositionForInterval(
        startTime: string,
        endTime: string,
        modeIndex: number,
        cellWidth: number
    ) {
        const startSeconds = this.timeToSeconds(startTime);
        const endSeconds = this.timeToSeconds(endTime);
        const durationSeconds = endSeconds - startSeconds;

        return {
            left: `${this.secondsToPixels(startSeconds, cellWidth)}px`,
            width: `${this.secondsToPixels(durationSeconds, cellWidth)}px`,
            top: `${this.TIME_HEIGHT + modeIndex * this.ROW_HEIGHT - 15}px`,
            height: `${this.ROW_HEIGHT - 10}px`
        };
    }
}
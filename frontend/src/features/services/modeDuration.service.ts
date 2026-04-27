import { DEFAULT_MODE_DURATION } from "$lib/config/schedule.config";

export class ModeDurationService {
    static getDurationForMode(
        modeId: number, 
        modeDurations: Record<string, number>, 
        modeIdToCode: Record<number, string>
    ): number {
        const modeCode = modeIdToCode[modeId];
        return modeCode && modeDurations[modeCode] !== undefined 
            ? modeDurations[modeCode] 
            : DEFAULT_MODE_DURATION; 
    }
}
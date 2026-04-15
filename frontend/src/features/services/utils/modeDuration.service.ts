export class ModeDurationService {
    static getDurationForMode(
        modeId: number, 
        modeDurations: Record<string, number>, 
        modeIdToCode: Record<number, string>
    ): number {
        const modeCode = modeIdToCode[modeId];
        return modeCode && modeDurations[modeCode] !== undefined 
            ? modeDurations[modeCode] 
            : 300; // дефолтное значение
    }
}
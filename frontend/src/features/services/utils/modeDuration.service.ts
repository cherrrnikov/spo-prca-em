export class ModeDurationService {
    private static BASE_URL = 'http://localhost:8081/api/schedule';

    static async loadModeDurations(): Promise<Record<string, number>> {
        try {
            const response = await fetch(`${this.BASE_URL}/mode-durations`);
            if (response.ok) {
                return await response.json();
            }
            return {};
        } catch (error) {
            console.error("Ошибка загрузки длительностей:", error);
            return {};
        }
    }

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
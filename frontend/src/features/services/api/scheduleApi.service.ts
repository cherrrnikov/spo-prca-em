import type {
    CreateProgramRequest,
    ForecastData,
    OperatorData
} from '$lib/types/schedule';

export class ScheduleApiService {
    private static BASE_URL = '/api';

    static async loadOperatorData(date: string): Promise<OperatorData> {
        const response = await fetch(`${this.BASE_URL}/schedule/proxy?date=${date}`);
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error("Нет данных для выбранной даты");
            }
            throw new Error(`Ошибка сервера: ${response.status}`);
        }
        return await response.json();
    }

    static async loadForecastData(date: string): Promise<ForecastData> {
        const response = await fetch(`${this.BASE_URL}/schedule/forecast-proxy?date=${date}`);
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error("Нет прогнозных данных для выбранной даты");
            }
            throw new Error(`Ошибка сервера: ${response.status}`);
        }
        const responseData = await response.json();
        
        return {
            main: responseData.forecast,
            shadows: responseData.shadows,
            zasvetki: responseData.zasvetki,
            totalIntervals: responseData.shadows.length + responseData.zasvetki.length
        };
    }

    static async saveProgram(programData: CreateProgramRequest): Promise<any> {
        const response = await fetch(`${this.BASE_URL}/programs/create`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(programData)
        });

        if (!response.ok) {
            throw new Error(`Ошибка сохранения: ${response.status}`);
        }

        return await response.json();
    }

    static async loadModeDurations(): Promise<Record<string, number>> {
        const response = await fetch('http://localhost:8081/api/schedule/mode-durations');
        if (!response.ok) return {};
        return await response.json();
    }
}
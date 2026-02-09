import type {
    CreateProgramRequest,
    ForecastData,
    Kr01DataResponse,
    OperatorData,
    Ro02DataResponse
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

    static async loadVkiData(date: string): Promise<Kr01DataResponse> {
        const response = await fetch(`${this.BASE_URL}/vki/correction/${date}`);
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error("Нет данных по коррекции орбиты для выбранной даты");
            }
            throw new Error(`Ошибка сервера при загрузке данных ВКИ: ${response.status}`);
        }
        return await response.json();
    }

    static async loadRotationData(date: string): Promise<Ro02DataResponse> {
        const response = await fetch(`${this.BASE_URL}/rotation/seasonal/${date}`);
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error("Нет данных по сезонным разворотам для выбранной даты");
            }
            throw new Error(`Ошибка сервера при загрузке данных разворотов: ${response.status}`);
        }
        return await response.json();
    }

    static async hasAstrocorrectionData(date: string): Promise<boolean> {
        try {
            const [vkiData, rotationData] = await Promise.allSettled([
                this.loadVkiData(date),
                this.loadRotationData(date)
            ]);
            
            const hasVkiData = vkiData.status === 'fulfilled' && 
                !(vkiData.value instanceof Error || vkiData.value === null);
            
            const hasRotationData = rotationData.status === 'fulfilled' && 
                !(rotationData.value instanceof Error || rotationData.value === null);
            
            console.log('Проверка астрокоррекции:', {
                date,
                hasVkiData,
                hasRotationData,
                vkiData: vkiData.status === 'fulfilled' ? 'успех' : 'ошибка',
                rotationData: rotationData.status === 'fulfilled' ? 'успех' : 'ошибка'
            });
            
            return hasVkiData || hasRotationData;
        } catch (error) {
            console.warn('Ошибка при проверке данных астрокоррекции:', error);
            return false;
        }
    }
}
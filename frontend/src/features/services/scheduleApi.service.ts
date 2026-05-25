import { loading } from '$lib/services/loading.service';
import type {
    CreateProgramRequest,
    ForecastData,
    Id02Dto,
    Kr01DataResponse,
    OperatorData,
    Ro02DataResponse,
    VpCreateRequest
} from '$lib/types';

export class ScheduleApiService {
    private static BASE_URL = '/proxy';

    static async loadOperatorData(date: string): Promise<OperatorData | null> {
        const response = await fetch(`${this.BASE_URL}/schedule/proxy?date=${date}`);
        if (!response.ok) {
            if (response.status === 404) {
                return null; // возвращаем null вместо ошибки
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
            total_intervals: responseData.shadows.length + responseData.zasvetki.length
        };
    }

    static async saveProgram(programData: CreateProgramRequest): Promise<{ numRp: number }> {
        loading.start();
        try {
            const response = await fetch(`${this.BASE_URL}/schedule/programs-proxy`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(programData)
            });
            if (!response.ok) {
                throw new Error(`Ошибка сохранения: ${response.status}`);
            }
            const text = await response.text();
            return text ? JSON.parse(text) : null;
        } finally {
            loading.stop();
        }
    }

    static async saveVp(vpData: VpCreateRequest): Promise<{vpId: number}> {
        loading.start();
        try {
            const response = await fetch(`${this.BASE_URL}/schedule/vp-proxy`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(vpData)
            });
            if (!response.ok) {
                throw new Error(`Ошибка сохранения: ${response.status}`);
            }
            const text = await response.text();
            return text ? JSON.parse(text) : null;
        } finally {
            loading.stop();
        }
    }

    static async generatePr01(numRp: number, numKa: number): Promise<string> {
        loading.start();
        try {
            const response = await fetch(`${this.BASE_URL}/schedule/pr01-proxy`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ numRp, numKa })
            });
            if (!response.ok) {
                throw new Error(`Ошибка генерации ПР01: ${response.status}`);
            }
            return await response.text();
        } finally {
            loading.stop();
        }
    }

    static async generatePr03(numRp: number, numKa: number): Promise<string> {
        loading.start();
        try {
            const response = await fetch(`${this.BASE_URL}/schedule/pr03-proxy`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ numRp, numKa })
            });
            if (!response.ok) {
                throw new Error(`Ошибка генерации ПР03: ${response.status}`);
            }
            return await response.text();
        } finally {
            loading.stop();
        }
    }

    static async generatePr04(numRp: number, numKa: number): Promise<string> {
        loading.start();
        try {
            const response = await fetch(`${this.BASE_URL}/schedule/pr04-proxy`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ numRp, numKa })
            });
            if (!response.ok) {
                throw new Error(`Ошибка генерации ПР04: ${response.status}`);
            }
            return await response.text();
        } finally {
            loading.stop();
        }
    }

    static async generateVp01(numRp: number, numKa: number): Promise<string> {
        loading.start();
        try {
            const response = await fetch(`${this.BASE_URL}/schedule/vp01-generate-proxy`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ numRp, numKa })
            });
            if (!response.ok) {
                throw new Error(`Ошибка генерации ВП01: ${response.status}`);
            }
            return await response.text();
        } finally {
            loading.stop();
        }
    }

    static async loadModeDurations(): Promise<Record<string, number>> {
        const response = await fetch(`${this.BASE_URL}/schedule/durations-proxy`);
        if (!response.ok) return {};
        return await response.json();
    }

    static async loadVkiData(date: string): Promise<Kr01DataResponse | null> {
        try {
            const response = await fetch(`${this.BASE_URL}/schedule/vki-proxy?date=${date}`);
            
            if (!response.ok) {
                if (response.status === 404) {
                    return null;
                }
                throw new Error(`Ошибка сервера при загрузке данных ВКИ: ${response.status}`);
            }
            
            const data = await response.json();

            return data;
            
        } catch (error) {
            console.warn(`Ошибка загрузки данных ВКИ для даты ${date}:`, error);
            return null;
        }
    }

    static async loadRotationData(date: string): Promise<Ro02DataResponse | null> {
        try {
            const response = await fetch(`${this.BASE_URL}/schedule/rotation-proxy?date=${date}`);
            
            if (!response.ok) {
                if (response.status === 404) {
                    return null;
                }
                throw new Error(`Ошибка сервера при загрузке данных разворотов: ${response.status}`);
            }
            
            const data = await response.json();
            return data;
            
        } catch (error) {
            console.warn(`Ошибка загрузки данных разворотов для даты ${date}:`, error);
            return null;
        }
    }

    static async loadBortData(date: string): Promise<Id02Dto | null> {
        try {
            const response = await fetch(`${this.BASE_URL}/schedule/bort-proxy?date=${date}`);

            if (!response.ok) {
                if (response.status === 404) {
                    return null;
                }
                throw new Error(`Ошибка сервера при загрузке данных о состоянии бортовых систем: ${response.status}`);
            }

            const data = await response.json();
            return data;
        } catch (error) {
            console.warn(`Ошибка загрузки данных о состоянии бортовых систем для даты ${date}: `, error);
            return null;
        }
    }

    static async hasAstrocorrectionData(date: string): Promise<boolean> {
        try {
            const [vkiData, rotationData] = await Promise.allSettled([
                this.loadVkiData(date),
                this.loadRotationData(date)
            ]);
            
            let hasVkiData = false;
            let hasRotationData = false;
            
            if (vkiData.status === 'fulfilled') {
                const data = vkiData.value;
                hasVkiData = data !== null && 
                                data.main !== null && 
                                data.impulses && 
                                data.impulses.length > 0;
            }
            
            if (rotationData.status === 'fulfilled') {
                const data = rotationData.value;
                hasRotationData = data !== null && 
                                    data.rotations && 
                                    data.rotations.length > 0;
            }
            
            return hasVkiData || hasRotationData;
            
        } catch (error) {
            console.warn('Ошибка при проверке данных астрокоррекции:', error);
            return false;
        }
    }

    static async loadAllDataForDate(date: string): Promise<{
        operatorData: OperatorData | null;
        forecastData: ForecastData | null;
        vkiData: Kr01DataResponse | null;
        rotationData: Ro02DataResponse | null;
    }> {
        loading.start();
        try {
            const [operatorData, forecastData, vkiData, rotationData] = await Promise.allSettled([
                this.loadOperatorData(date).catch(() => null),
                this.loadForecastData(date).catch(() => null),
                this.loadVkiData(date).catch(() => null),
                this.loadRotationData(date).catch(() => null)
            ]);
            return {
                operatorData: operatorData.status === 'fulfilled' ? operatorData.value : null,
                forecastData: forecastData.status === 'fulfilled' ? forecastData.value : null,
                vkiData: vkiData.status === 'fulfilled' ? vkiData.value : null,
                rotationData: rotationData.status === 'fulfilled' ? rotationData.value : null
            };
        } finally {
            loading.stop();
        }
    }
}
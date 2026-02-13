import type {
    ForecastData,
    Kr01DataResponse,
    Kr01ImpulseDto,
    Ro02DataResponse,
    Ro02Dto,
    RotationInterval,
    ShadowInterval,
    TsMsuConfig,
    VkiInterval,
    ZasvetkaInterval
} from '$lib/types/schedule';
import { TimeUtils } from '$lib/utils/time';

export class ScheduleConverterService {
    static getDefaultMsuConfig(): TsMsuConfig {
        return {
            prMsu: 0,
            prVdMsu: 0,
            prIkMsu: 0,
            vd1: 0,
            vd2: 0,
            vd3: 0,
            ik4: 0,
            ik5: 0,
            ik6: 0,
            ik7: 0,
            ik8: 0,
            ik9: 0,
            ik10: 0
        };
    }

    static formatTimeFromISO(isoString: string): string {
        try {
            const date = new Date(isoString);
            return date.toTimeString().substring(0, 5); 
        } catch {
            return "00:00";
        }
    }

    static formatDateTime(dateStr: string): string {
        try {
            const date = new Date(dateStr);
            return date.toLocaleString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch {
            return dateStr;
        }
    }

    static formatTimeOnly(dateStr: string): string {
        return TimeUtils.extractTimeFromTimestamp(dateStr);
    }

    static convertForecastToIntervals(forecastData: ForecastData): {
        shadows: ShadowInterval[],
        zasvetki: ZasvetkaInterval[]
    } {
        return {
            shadows: forecastData.shadows.map(shadow => ({
                id: `shadow_${shadow.id}`,
                type: 'shadow',
                startTime: TimeUtils.extractTimeFromTimestamp(shadow.dTIn),
                endTime: TimeUtils.extractTimeFromTimestamp(shadow.dTOut),
                duration: shadow.duration,
                title: 'Тень',
                color: 'rgba(83, 83, 83, 1)',
                opacity: 1,
                zIndex: 2
            })),
            zasvetki: forecastData.zasvetki.map(zasvetka => ({
                id: `zasvetka_${zasvetka.id}`,
                type: 'zasvetka',
                startTime: TimeUtils.extractTimeFromTimestamp(zasvetka.dTIn),
                endTime: TimeUtils.extractTimeFromTimestamp(zasvetka.dTOut),
                duration: zasvetka.duration,
                title: 'Засветка',
                color: 'rgba(175, 175, 175, 1)',
                opacity: 1,
                zIndex: 1
            }))
        };
    }

    static convertVkiToIntervals(vkiData: Kr01DataResponse | null): VkiInterval[] {
        if (!vkiData?.impulses || vkiData.impulses.length === 0) {
            return [];
        }

        return vkiData.impulses.map((impulse: Kr01ImpulseDto, index: number) => {
            const time = this.extractTimeOnly(impulse.dateIm);
            const date = impulse.dateIm.split('T')[0];
            
            // Длительность 5 минут (300 секунд) фиксированная для отображения
            const duration = impulse.dlit || 300;
            const endTime = TimeUtils.calculateEndTime(time, duration / 60);
            
            return {
                id: `vki-${date}-${index + 1}`,
                type: 'vki',
                startTime: time,
                endTime: endTime,
                duration: duration,
                title: `ВКИ`,
                color: '#000000',
                opacity: 1,
                zIndex: 1,
                impulseNumber: index + 1,
                mass: impulse.massa,
                angle: impulse.uglV,
                nVit: impulse.nVit,
                nDu: impulse.nDu
            };
        });
    }

    static convertRotationToIntervals(rotationData: Ro02DataResponse | null, targetDate: string): RotationInterval[] {
        if (!rotationData?.rotations || rotationData.rotations.length === 0) {
            return [];
        }

        return rotationData.rotations
            .filter((rotation: Ro02Dto) => {
                // Рисуем разворот ТОЛЬКО если дата запроса совпадает с dataRazv
                const rotationDate = rotation.dataRazv.split('T')[0];
                return rotationDate === targetDate;
            })
            .map((rotation: Ro02Dto, index: number) => {
                const time = this.extractTimeOnly(rotation.dataRazv);
                const date = rotation.dataRazv.split('T')[0];
                
                const duration = 300;
                const endTime = TimeUtils.calculateEndTime(time, duration / 60);
                
                return {
                    id: `rotation-${date}-${index + 1}`,
                    type: 'rotation',
                    startTime: time,
                    endTime: endTime,
                    duration: duration,
                    title: `Сезонный разворот`,
                    color: '#000000',
                    opacity: 1,
                    zIndex: 1,
                    rotationNumber: index + 1
                };
            });
    }

    static createDateWithTime(baseDate: Date, timeStr: string): Date {
        const date = new Date(baseDate);
        
        // Если timeStr уже полный timestamp
        if (timeStr.includes('T') || timeStr.includes('-')) {
            try {
                return new Date(timeStr);
            } catch {
                // Если не удалось распарсить, продолжаем
            }
        }
        
        // Если только время HH:MM
        const [hours, minutes] = timeStr.split(':').map(Number);
        date.setHours(hours || 0, minutes || 0, 0, 0);
        return date;
    }

    static extractTimeOnly(timeStr: string): string {
        try {
            if (timeStr.includes('T') || timeStr.includes('-')) {
                const date = new Date(timeStr);
                return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`;
            }
            
            const timeRegex = /^(\d{1,2}):(\d{2})(:(\d{2}))?$/;
            const match = timeStr.match(timeRegex);
            
            if (match) {
                const hours = parseInt(match[1]).toString().padStart(2, '0');
                const minutes = match[2];
                const seconds = match[4] ? match[4].padStart(2, '0') : '00';
                return `${hours}:${minutes}:${seconds}`;
            }
            
            return "00:00:00";
        } catch {
            return "00:00:00";
        }
    }
}
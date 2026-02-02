import type {
    ForecastData,
    ShadowInterval,
    TsMsuConfig,
    ZasvetkaInterval
} from '$lib/types/schedule';

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
        try {
            const date = new Date(dateStr);
            return date.toLocaleTimeString('ru-RU', {
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch {
            return dateStr;
        }
    }

    static convertForecastToIntervals(forecastData: ForecastData): {
        shadows: ShadowInterval[],
        zasvetki: ZasvetkaInterval[]
    } {
        return {
            shadows: forecastData.shadows.map(shadow => ({
                id: `shadow_${shadow.id}`,
                type: 'shadow',
                startTime: this.formatTimeFromISO(shadow.dTIn),
                endTime: this.formatTimeFromISO(shadow.dTOut),
                duration: shadow.duration,
                title: 'Тень',
                color: 'rgba(83, 83, 83, 1)',
                opacity: 1,
                zIndex: 2
            })),
            zasvetki: forecastData.zasvetki.map(zasvetka => ({
                id: `zasvetka_${zasvetka.id}`,
                type: 'zasvetka',
                startTime: this.formatTimeFromISO(zasvetka.dTIn),
                endTime: this.formatTimeFromISO(zasvetka.dTOut),
                duration: zasvetka.duration,
                title: 'Засветка',
                color: 'rgba(175, 175, 175, 1)',
                opacity: 1,
                zIndex: 1
            }))
        };
    }

    static createDateWithTime(baseDate: Date, timeStr: string): Date {
        const date = new Date(baseDate);
        
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
                return date.getHours().toString().padStart(2, '0') + ':' + 
                       date.getMinutes().toString().padStart(2, '0');
            }
            
            const timeRegex = /^(\d{1,2}):(\d{2})$/;
            const match = timeStr.match(timeRegex);
            
            if (match) {
                const hours = parseInt(match[1]).toString().padStart(2, '0');
                const minutes = match[2];
                return `${hours}:${minutes}`;
            }
            
            return "00:00";
        } catch {
            return "00:00";
        }
    }
}
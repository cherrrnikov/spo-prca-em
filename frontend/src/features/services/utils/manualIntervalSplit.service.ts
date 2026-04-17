import { MODE_CODES } from '$lib/constants/schedule';
import { MsuMapper } from '$lib/mappers/msuMapper';
import type { ModeCreationForm, TimeInterval } from '$lib/types';
import { getDefaultIntervalFlags } from '$lib/utils/interval';
import { CityService } from './cities.service';

export class ManualIntervalSplitService {
    
    static splitInterval(
        formData: ModeCreationForm,
        date: string,
        tempIdPrefix: string = 'manual'
    ): TimeInterval[] {
        const subIntervals: TimeInterval[] = [];
        
        // Определяем шаг в зависимости от типа съемки (1=штатная 30мин, 2=учащенная 15мин)
        const stepMinutes = formData.tip === 1 ? 30 : 15;
        const intervalDuration = formData.duration * 1000; // длительность каждого маленького интервала в секундах
        const intervalDurationSec = formData.duration;

        const city = CityService.getCityByPpi(formData.ppiNum);
        const color = CityService.getColorByPpi(formData.ppiNum);
        
        const startDate = new Date(`${date}T${formData.startTime}`);
        const endDate = new Date(`${date}T${formData.endTime}`);
        
        let currentTime = new Date(startDate);
        let index = 0;
        
        while (currentTime.getTime() + intervalDuration <= endDate.getTime()) {
            const subStartTime = new Date(currentTime);
            const subEndTime = new Date(subStartTime.getTime() + intervalDuration);
            
            const formatTime = (date: Date): string => {
                const hours = date.getHours().toString().padStart(2, '0');
                const minutes = date.getMinutes().toString().padStart(2, '0');
                const seconds = date.getSeconds().toString().padStart(2, '0');
                return `${hours}:${minutes}:${seconds}`;
            };
            
            const subInterval: TimeInterval = {
                id: `${tempIdPrefix}_${Date.now()}_${index}_${Math.random().toString(36).substr(2, 5)}`,
                mode: formData.modeType!,
                date: date,
                startTime: formatTime(subStartTime),
                endTime: formatTime(subEndTime),
                city,
                color,
                ppi: formData.ppiNum,
                dlit: intervalDurationSec,
                customerCode: formData.customerCode,
                
                // Параметры МСУ для съемок
                msu1Config: { ...formData.msu1Config },
                msu2Config: { ...formData.msu2Config },
                
                // Параметры для ТС (БССД, ЗГ, отключение ЗГ)
                msuData: (formData.modeType === MODE_CODES.TS || formData.modeType === MODE_CODES.SHOOTING) ? MsuMapper.fromForm(
                    formData, {
                        id: 0,
                        idMain: 0,
                        dlit: intervalDuration
                    }
                ) : undefined,
                
                ...getDefaultIntervalFlags()
            };
            
            subIntervals.push(subInterval);
            
            currentTime.setMinutes(currentTime.getMinutes() + stepMinutes);
            index++;
        }
        
        return subIntervals;
    }
}
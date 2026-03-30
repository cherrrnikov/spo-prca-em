import type { ModeCreationForm, TimeInterval } from '$lib/types';
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
                tsData: (formData.modeType === 8 || formData.modeType === 1) ? {
                    id: 0,
                    idMain: 0,
                    tip: formData.tip ?? 1,
                    reg: formData.reg ?? 0,
                    dlit: intervalDuration,
                    prMsu1: formData.msu1Config.prMsu || 0,
                    vd1Msu1: formData.msu1Config.vd1 || 0,
                    vd2Msu1: formData.msu1Config.vd2 || 0,
                    vd3Msu1: formData.msu1Config.vd3 || 0,
                    ik4Msu1: formData.msu1Config.ik4 || 0,
                    ik5Msu1: formData.msu1Config.ik5 || 0,
                    ik6Msu1: formData.msu1Config.ik6 || 0,
                    ik7Msu1: formData.msu1Config.ik7 || 0,
                    ik8Msu1: formData.msu1Config.ik8 || 0,
                    ik9Msu1: formData.msu1Config.ik9 || 0,
                    ik10Msu1: formData.msu1Config.ik10 || 0,
                    prMsu2: formData.msu2Config.prMsu || 0,
                    vd1Msu2: formData.msu2Config.vd1 || 0,
                    vd2Msu2: formData.msu2Config.vd2 || 0,
                    vd3Msu2: formData.msu2Config.vd3 || 0,
                    ik4Msu2: formData.msu2Config.ik4 || 0,
                    ik5Msu2: formData.msu2Config.ik5 || 0,
                    ik6Msu2: formData.msu2Config.ik6 || 0,
                    ik7Msu2: formData.msu2Config.ik7 || 0,
                    ik8Msu2: formData.msu2Config.ik8 || 0,
                    ik9Msu2: formData.msu2Config.ik9 || 0,
                    ik10Msu2: formData.msu2Config.ik10 || 0,
                    prBssd: formData.prBssd ?? 0,
                    prZg: formData.prZg ?? 0,
                    prOtklZgBssd: formData.prOtklZg ?? 0
                } : undefined,
                
                hasConflict: false,
                conflictWith: [],
                willBeSaved: true,
                nearZasvetka: false,
                zasvetkaConflict: false,
                zasvetkaDistance: 0
            };
            
            subIntervals.push(subInterval);
            
            currentTime.setMinutes(currentTime.getMinutes() + stepMinutes);
            index++;
        }
        
        return subIntervals;
    }
}
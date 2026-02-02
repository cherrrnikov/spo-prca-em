import type { Id06TsDto, PpiAssignment, TimeInterval, TsMsuConfig } from '$lib/types/schedule';
import { CityService } from '../utils/cities.service';

export class TsIntervalService {
    static convertTsToSubIntervals(
        tsRecord: Id06TsDto,
        ppiAssignments: PpiAssignment[]
    ): TimeInterval[] {
        const subIntervals: TimeInterval[] = [];
        
        const assignment = ppiAssignments.find(
            a => a.recordId === tsRecord.id && a.recordType === 'ts'
        );
        
        const ppiNum = assignment?.ppiNum || 1;
        const city = CityService.getCityByPpi(ppiNum);
        const color = CityService.getColorByPpi(ppiNum);
        
        const stepMinutes = tsRecord.tip === 0 ? 30 : 15;
        const subIntervalDuration = 7; 
        
        const startDate = new Date(tsRecord.dn);
        const endDate = new Date(tsRecord.dk);
        
        let currentTime = new Date(startDate);
        
        // Генерируем подынтервалы до тех пор, пока следующий интервал не выйдет за границы
        while (currentTime.getTime() + (subIntervalDuration * 60000) <= endDate.getTime()) {
            const subStartTime = new Date(currentTime);
            const subEndTime = new Date(subStartTime.getTime() + (subIntervalDuration * 60000));
            
            const formatTime = (date: Date): string => {
                const hours = date.getHours().toString().padStart(2, '0');
                const minutes = date.getMinutes().toString().padStart(2, '0');
                return `${hours}:${minutes}`;
            };
            
            const subInterval: TimeInterval = {
                id: `ts_${tsRecord.id}_${formatTime(subStartTime)}`,
                mode: 8, 
                startTime: formatTime(subStartTime),
                endTime: formatTime(subEndTime),
                city,
                color,
                title: `Техн. съёмка (tip=${tsRecord.tip}, ППИ ${ppiNum})`,
                description: `Технологическая съемка, тип: ${tsRecord.tip}, режим: ${tsRecord.reg}`,
                ppi: ppiNum,
                dlit: subIntervalDuration * 60, 
                customerCode: 5, 
                
                msu1Config: this.getMsuConfigFromTsRecord(tsRecord, 1),
                msu2Config: this.getMsuConfigFromTsRecord(tsRecord, 2),
                
                hasConflict: false,
                conflictWith: [],
                willBeSaved: true,
                nearZasvetka: false,
                zasvetkaConflict: false,
                zasvetkaDistance: 0
            };
            
            subIntervals.push(subInterval);
            
            currentTime.setMinutes(currentTime.getMinutes() + stepMinutes);
        }
        
        return subIntervals;
    }

    private static getMsuConfigFromTsRecord(tsRecord: Id06TsDto, msuNumber: 1 | 2): TsMsuConfig {
        if (msuNumber === 1) {
            return {
                prMsu: tsRecord.prMsu1 || 0,
                prVdMsu: tsRecord.prVdMsu1 || 0,
                prIkMsu: tsRecord.prIkMsu1 || 0,
                vd1: tsRecord.prVd1_1 || 0,
                vd2: tsRecord.prVd2_1 || 0,
                vd3: tsRecord.prVd3_1 || 0,
                ik4: tsRecord.prIk4_1 || 0,
                ik5: tsRecord.prIk5_1 || 0,
                ik6: tsRecord.prIk6_1 || 0,
                ik7: tsRecord.prIk7_1 || 0,
                ik8: tsRecord.prIk8_1 || 0,
                ik9: tsRecord.prIk9_1 || 0,
                ik10: tsRecord.prIk10_1 || 0
            };
        } else {
            return {
                prMsu: tsRecord.prMsu2 || 0,
                prVdMsu: tsRecord.prVdMsu2 || 0,
                prIkMsu: tsRecord.prIkMsu2 || 0,
                vd1: tsRecord.prVd1_2 || 0,
                vd2: tsRecord.prVd2_2 || 0,
                vd3: tsRecord.prVd3_2 || 0,
                ik4: tsRecord.prIk4_2 || 0,
                ik5: tsRecord.prIk5_2 || 0,
                ik6: tsRecord.prIk6_2 || 0,
                ik7: tsRecord.prIk7_2 || 0,
                ik8: tsRecord.prIk8_2 || 0,
                ik9: tsRecord.prIk9_2 || 0,
                ik10: tsRecord.prIk10_2 || 0
            };
        }
    }
}
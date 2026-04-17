import { MsuMapper } from '$lib/mappers/msuMapper';
import type { Id02Dto, Id06TsDto, MsuConfig, TimeInterval } from '$lib/types';
import { getDefaultIntervalFlags } from '$lib/utils/interval';
import { CityService } from '../utils/cities.service';

export class ShootingIntervalService {
    static convertTsToSubIntervals(
        tsRecord: Id06TsDto,
        ppiNum: number,
        bortData?: Id02Dto | null  // ← добавить параметр
    ): TimeInterval[] {
        const subIntervals: TimeInterval[] = [];
        
        const city = CityService.getCityByPpi(ppiNum);
        const color = CityService.getColorByPpi(ppiNum);
        
        const stepMinutes = tsRecord.tip === 1 ? 30 : 15;
        const subIntervalDuration = 7; 
        
        const startDate = new Date(tsRecord.dn);
        const endDate = new Date(tsRecord.dk);
        
        let currentTime = new Date(startDate);
        
        while (currentTime.getTime() + (subIntervalDuration * 60000) <= endDate.getTime()) {
            const subStartTime = new Date(currentTime);
            const subEndTime = new Date(subStartTime.getTime() + (subIntervalDuration * 60000));
            
            const formatTime = (date: Date): string => {
                const hours = date.getHours().toString().padStart(2, '0');
                const minutes = date.getMinutes().toString().padStart(2, '0');
                const seconds = date.getSeconds().toString().padStart(2, '0');
                return `${hours}:${minutes}:${seconds}`;
            };

            const date = tsRecord.dn.split('T')[0];
            
            const subInterval: TimeInterval = {
                id: `ts_${tsRecord.id}_${formatTime(subStartTime)}`,
                mode: 8, 
                date: date,
                startTime: formatTime(subStartTime),
                endTime: formatTime(subEndTime),
                city,
                color,
                ppi: ppiNum,
                dlit: subIntervalDuration * 60, 
                customerCode: 5, 
                
                msu1Config: this.getMsuConfigFromTsRecord(tsRecord, 1),
                msu2Config: this.getMsuConfigFromTsRecord(tsRecord, 2),
                
                msuData: MsuMapper.fromId06(
                    tsRecord,
                    tsRecord.id_main,
                    bortData ?? null,
                    subIntervalDuration * 60
                ),
                
                ...getDefaultIntervalFlags()
            };
            
            subIntervals.push(subInterval);
            
            currentTime.setMinutes(currentTime.getMinutes() + stepMinutes);
        }
        
        return subIntervals;
    }

    private static getMsuConfigFromTsRecord(tsRecord: Id06TsDto, msuNumber: 1 | 2): MsuConfig {
        if (msuNumber === 1) {
            return {
                prMsu: tsRecord.pr_msu1 || 0,
                prVdMsu: tsRecord.pr_vd_msu1 || 0,
                prIkMsu: tsRecord.pr_ik_msu1 || 0,
                vd1: tsRecord.pr_vd1_1 || 0,
                vd2: tsRecord.pr_vd2_1 || 0,
                vd3: tsRecord.pr_vd3_1 || 0,
                ik4: tsRecord.pr_ik4_1 || 0,
                ik5: tsRecord.pr_ik5_1 || 0,
                ik6: tsRecord.pr_ik6_1 || 0,
                ik7: tsRecord.pr_ik7_1 || 0,
                ik8: tsRecord.pr_ik8_1 || 0,
                ik9: tsRecord.pr_ik9_1 || 0,
                ik10: tsRecord.pr_ik10_1 || 0
            };
        } else {
            return {
                prMsu: tsRecord.pr_msu2 || 0,
                prVdMsu: tsRecord.pr_vd_msu2 || 0,
                prIkMsu: tsRecord.pr_ik_msu2 || 0,
                vd1: tsRecord.pr_vd1_2 || 0,
                vd2: tsRecord.pr_vd2_2 || 0,
                vd3: tsRecord.pr_vd3_2 || 0,
                ik4: tsRecord.pr_ik4_2 || 0,
                ik5: tsRecord.pr_ik5_2 || 0,
                ik6: tsRecord.pr_ik6_2 || 0,
                ik7: tsRecord.pr_ik7_2 || 0,
                ik8: tsRecord.pr_ik8_2 || 0,
                ik9: tsRecord.pr_ik9_2 || 0,
                ik10: tsRecord.pr_ik10_2 || 0
            };
        }
    }
}
import { RECORD_TYPES } from '$lib/constants/recordTypes';
import { MODE_CODES } from '$lib/constants/schedule';
import type {
    Id02Dto,
    Id06KvdDto,
    Id06OnaDto,
    Id06TnpDto,
    OperatorData,
    PpiAssignment,
    TimeInterval,
    WorkMode
} from '$lib/types';
import { getDefaultIntervalFlags } from '$lib/utils/interval';
import { TimeUtils } from '$lib/utils/time';
import { CityService } from './cities.service';
import { ScheduleConverterService } from './scheduleConverter.service';
import { ShootingIntervalService } from './shootingInterval.service';

export class ScheduleCreationService {
    // Вспомогательная функция для получения номера ППИ
    private static getPpiNumber(
        recordId: number, 
        recordType: string, 
        ppiAssignments: PpiAssignment[],
        defaultPpi?: number
    ): number {
        // Если есть конкретное назначение - используем его
        const assignment = ppiAssignments.find(a => 
            a.recordId === recordId && a.recordType === recordType
        );
        if (assignment) return assignment.ppiNum;
        
        // Если есть дефолтное значение - используем его
        if (defaultPpi !== undefined) return defaultPpi;
        
        // Если ничего нет - возвращаем 1
        return 1;
    }

    static convertToTimeIntervals(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        workModes: WorkMode[],
        defaultPpi?: number,
        bortData?: Id02Dto | null
    ): TimeInterval[] {
        const intervals: TimeInterval[] = [];
        
        if (operatorData.kvd_list?.length) {
            operatorData.kvd_list.forEach(kvd => {
                const ppiNum = this.getPpiNumber(kvd.id, RECORD_TYPES.KVD, ppiAssignments, defaultPpi);
                intervals.push(this.createKvdInterval(kvd, ppiNum, operatorData.main?.k_zajv));
            });
        }
        
        if (operatorData.tnp_list?.length) {
            operatorData.tnp_list.forEach(tnp => {
                const ppiNum = this.getPpiNumber(tnp.id, RECORD_TYPES.TNP, ppiAssignments, defaultPpi);
                intervals.push(this.createTnpInterval(tnp, ppiNum, operatorData.main?.k_zajv));
            });
        }
        
        if (operatorData.ts_list?.length) {
            operatorData.ts_list.forEach(ts => {
                const ppiNum = this.getPpiNumber(ts.id, RECORD_TYPES.TS, ppiAssignments, defaultPpi);
                const tsSubIntervals = ShootingIntervalService.convertTsToSubIntervals(ts, ppiNum, bortData);
                intervals.push(...tsSubIntervals);
            });
        }

        if (operatorData.ona_list?.length) {
            operatorData.ona_list.forEach(ona => {
                const ppiNum = this.getPpiNumber(ona.id, RECORD_TYPES.ONA, ppiAssignments, defaultPpi);
                intervals.push(this.createOnaInterval(ona, ppiNum, operatorData.main?.k_zajv));
            });
        }
        
        return intervals.map(interval => this.applyDefaultIntervalValues(interval, operatorData.main?.k_zajv));
    }

    private static createKvdInterval(kvd: Id06KvdDto, ppiNum: number, customerCode?: number): TimeInterval {
        const date = kvd.dn.split('T')[0];
        return {
            id: `kvd_${kvd.id}`,
            mode: MODE_CODES.KVD,
            date: date,
            startTime: TimeUtils.extractTimeFromTimestamp(kvd.dn), 
            endTime: TimeUtils.extractTimeFromTimestamp(kvd.dk),
            city: CityService.getCityByPpi(ppiNum),
            color: CityService.getColorByPpi(ppiNum),
            title: `Калибровка ВД (ППИ ${ppiNum})`,
            description: `Калибровка ВД, ID: ${kvd.id}`,
            ppi: ppiNum,
            dlit: TimeUtils.calculateDuration(kvd.dn, kvd.dk),
            customerCode: customerCode || 5,
            ...getDefaultIntervalFlags(),
            kvdConfig: {
                prMsu: kvd.pr_msu,
                prBssd: kvd.pr_bssd,
                prZg: kvd.pr_zg
            }
        };
    }

    private static createTnpInterval(tnp: Id06TnpDto, ppiNum: number, customerCode?: number): TimeInterval {
        const date = tnp.dn.split('T')[0];
        return {
            id: `tnp_${tnp.id}`,
            mode: MODE_CODES.TNP,
            date: date,
            startTime: TimeUtils.extractTimeFromTimestamp(tnp.dn), 
            endTime: TimeUtils.extractTimeFromTimestamp(tnp.dk), 
            city: CityService.getCityByPpi(ppiNum),
            color: CityService.getColorByPpi(ppiNum),
            title: `ТНП (ППИ ${ppiNum})`,
            description: `Режим ТНП, длительность: ${tnp.dlit} сек`,
            ppi: ppiNum,
            dlit: tnp.dlit,
            customerCode: customerCode || 5,
            ...getDefaultIntervalFlags()
        };
    }

    private static createOnaInterval(
        ona: Id06OnaDto,
        ppiNum: number,
        customerCode?: number
    ): TimeInterval {
        const date = ona.dn.split('T')[0];
        return {
            id: `ona_${ona.id}`,
            mode: MODE_CODES.ONA,  
            date: date,
            startTime: TimeUtils.extractTimeFromTimestamp(ona.dn),
            endTime: TimeUtils.extractTimeFromTimestamp(ona.dk),
            city: CityService.getCityByPpi(ppiNum),
            color: CityService.getColorByPpi(ppiNum),
            title: `Юстировка ОНА (Антенна ${ona.n_ona})`,
            description: `Юстировка ОНА, длительность: ${ona.dlit} сек`,
            ppi: ppiNum,
            dlit: ona.dlit,
            nOna: ona.n_ona,  
            customerCode: customerCode || 5,
            ...getDefaultIntervalFlags()
        };
    }

    private static applyDefaultIntervalValues(interval: TimeInterval, customerCode?: number): TimeInterval {
        return {
            ...interval,
            msu1Config: interval.msu1Config || ScheduleConverterService.getDefaultMsuConfig(),
            msu2Config: interval.msu2Config || ScheduleConverterService.getDefaultMsuConfig(),
            customerCode: interval.customerCode || customerCode || 1,
            nOna: interval.nOna || (interval.mode === MODE_CODES.ONA ? 1 : undefined),
            ...getDefaultIntervalFlags()
        };
    }
}
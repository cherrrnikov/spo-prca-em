import type {
    CreatedProgramData,
    CreateProgramRequest,
    ForecastData,
    Id02Dto,
    Id06OnaDto,
    Kr01DataResponse,
    OperatorData,
    PpiAssignment,
    Ro02DataResponse,
    RotationInterval,
    TimeInterval,
    VkiInterval,
    WorkMode
} from '$lib/types';
import { TimeUtils } from '$lib/utils/time';
import type { ScheduleStatus } from '../schedule-creation/types';
import { ScheduleApiService } from './api/scheduleApi.service';
import { ProgramPreparerService } from './data/programPreparer.service';
import { ScheduleConverterService } from './data/scheduleConverter.service';
import { ShootingIntervalService } from './data/shootingInterval.service';
import { CityService } from './utils/cities.service';

export class ScheduleCreationService {
    static async loadOperatorData(date: string): Promise<OperatorData> {
        return ScheduleApiService.loadOperatorData(date);
    }

    static async saveProgram(programData: CreateProgramRequest): Promise<any> {
        return ScheduleApiService.saveProgram(programData);
    }

    static async loadForecastData(date: string): Promise<ForecastData> {
        return ScheduleApiService.loadForecastData(date);
    }

    static prepareProgramData(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        selectedDate: string,
        selectedTime: string,
        scheduleStatus: ScheduleStatus
    ): CreateProgramRequest {
        return ProgramPreparerService.prepareProgramData(
            operatorData,
            ppiAssignments,
            selectedDate,
            selectedTime,
            scheduleStatus
        );
    }

    static prepareFullProgramData(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        createdPrograms: CreatedProgramData[],
        selectedDate: string,
        selectedTime: string,
        scheduleStatus: ScheduleStatus,
        numKa: number,
        numRp?: number
    ): CreateProgramRequest {
        return ProgramPreparerService.prepareFullProgramData(
            operatorData,
            ppiAssignments,
            createdPrograms,
            selectedDate,
            selectedTime,
            scheduleStatus,
            numKa,
            numRp
        );
    }

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
                const ppiNum = this.getPpiNumber(kvd.id, 'kvd', ppiAssignments, defaultPpi);
                intervals.push(this.createKvdInterval(kvd, ppiNum, operatorData.main?.k_zajv));
            });
        }
        
        if (operatorData.tnp_list?.length) {
            operatorData.tnp_list.forEach(tnp => {
                const ppiNum = this.getPpiNumber(tnp.id, 'tnp', ppiAssignments, defaultPpi);
                intervals.push(this.createTnpInterval(tnp, ppiNum, operatorData.main?.k_zajv));
            });
        }
        
        if (operatorData.ts_list?.length) {
            operatorData.ts_list.forEach(ts => {
                const ppiNum = this.getPpiNumber(ts.id, 'ts', ppiAssignments, defaultPpi);
                const tsSubIntervals = ShootingIntervalService.convertTsToSubIntervals(ts, ppiNum, bortData);
                intervals.push(...tsSubIntervals);
            });
        }

        if (operatorData.ona_list?.length) {
            operatorData.ona_list.forEach(ona => {
                const ppiNum = this.getPpiNumber(ona.id, 'ona', ppiAssignments, defaultPpi);
                intervals.push(this.createOnaInterval(ona, ppiNum, operatorData.main?.k_zajv));
            });
        }
        
        return intervals.map(interval => this.applyDefaultIntervalValues(interval, operatorData.main?.k_zajv));
    }

    static getCustomerLabel(code: number): string {
        return ProgramPreparerService.getCustomerLabel(code);
    }

    static generateProgramNumber(): number {
        return ProgramPreparerService.generateProgramNumber();
    }

    static calculateDateOff(
        operatorData: OperatorData,
        selectedDate: string,
        selectedTime: string
    ): string {
        return ProgramPreparerService.calculateDateOff(operatorData, selectedDate, selectedTime);
    }

    static calculateDuration(startStr: string, endStr: string): number {
        return TimeUtils.calculateDuration(startStr, endStr);
    }

    static formatDateTime(dateStr: string): string {
        return TimeUtils.formatDateTime(dateStr);
    }

    static formatTimeOnly(dateStr: string): string {
        return TimeUtils.formatTimeOnly(dateStr);
    }

    static getDefaultMsuConfig() {
        return ScheduleConverterService.getDefaultMsuConfig();
    }

    static getCityByPpi(ppiNum: number): string {
        return CityService.getCityByPpi(ppiNum);
    }

    static getColorByPpi(ppiNum: number): string {
        return CityService.getColorByPpi(ppiNum);
    }

    static convertForecastToIntervals(forecastData: ForecastData) {
        return ScheduleConverterService.convertForecastToIntervals(forecastData);
    }

    static async loadVkiData(date: string): Promise<Kr01DataResponse | null> {
        return ScheduleApiService.loadVkiData(date);
    }

    static async loadRotationData(date: string): Promise<Ro02DataResponse | null> {
        return ScheduleApiService.loadRotationData(date);
    }

    static convertVkiToIntervals(vkiData: Kr01DataResponse | null): VkiInterval[] {
        return ScheduleConverterService.convertVkiToIntervals(vkiData);
    }

    static convertRotationToIntervals(rotationData: Ro02DataResponse | null, targetDate: string): RotationInterval[] {
        return ScheduleConverterService.convertRotationToIntervals(rotationData, targetDate);
    }

    private static createKvdInterval(kvd: any, ppiNum: number, customerCode?: number): TimeInterval {
        const date = kvd.dn.split('T')[0];
        return {
            id: `kvd_${kvd.id}`,
            mode: 7,
            date: date,
            startTime: TimeUtils.extractTimeFromTimestamp(kvd.dn), 
            endTime: TimeUtils.extractTimeFromTimestamp(kvd.dk),
            city: this.getCityByPpi(ppiNum),
            color: this.getColorByPpi(ppiNum),
            title: `Калибровка ВД (ППИ ${ppiNum})`,
            description: `Калибровка ВД, ID: ${kvd.id}`,
            ppi: ppiNum,
            dlit: this.calculateDuration(kvd.dn, kvd.dk),
            customerCode: customerCode || 5,
            hasConflict: false,
            conflictWith: [],
            nearZasvetka: false,
            zasvetkaConflict: false,
            zasvetkaDistance: 0,
            willBeSaved: true,
            kvdConfig: {
                prMsu: kvd.pr_msu,
                prBssd: kvd.pr_bssd,
                prZg: kvd.pr_zg
            }
        };
    }

    private static createTnpInterval(tnp: any, ppiNum: number, customerCode?: number): TimeInterval {
        const date = tnp.dn.split('T')[0];
        return {
            id: `tnp_${tnp.id}`,
            mode: 4,
            date: date,
            startTime: TimeUtils.extractTimeFromTimestamp(tnp.dn), 
            endTime: TimeUtils.extractTimeFromTimestamp(tnp.dk), 
            city: this.getCityByPpi(ppiNum),
            color: this.getColorByPpi(ppiNum),
            title: `ТНП (ППИ ${ppiNum})`,
            description: `Режим ТНП, длительность: ${tnp.dlit} сек`,
            ppi: ppiNum,
            dlit: tnp.dlit,
            customerCode: customerCode || 5,
            hasConflict: false,
            conflictWith: [],
            nearZasvetka: false,
            zasvetkaConflict: false,
            zasvetkaDistance: 0,
            willBeSaved: true
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
            mode: 6,  
            date: date,
            startTime: TimeUtils.extractTimeFromTimestamp(ona.dn),
            endTime: TimeUtils.extractTimeFromTimestamp(ona.dk),
            city: this.getCityByPpi(ppiNum),
            color: this.getColorByPpi(ppiNum),
            title: `Юстировка ОНА (Антенна ${ona.n_ona})`,
            description: `Юстировка ОНА, длительность: ${ona.dlit} сек`,
            ppi: ppiNum,
            dlit: ona.dlit,
            nOna: ona.n_ona,  
            customerCode: customerCode || 5,
            hasConflict: false,
            conflictWith: [],
            nearZasvetka: false,
            zasvetkaConflict: false,
            zasvetkaDistance: 0,
            willBeSaved: true
        };
    }

    private static applyDefaultIntervalValues(interval: TimeInterval, customerCode?: number): TimeInterval {
        return {
            ...interval,
            msu1Config: interval.msu1Config || this.getDefaultMsuConfig(),
            msu2Config: interval.msu2Config || this.getDefaultMsuConfig(),
            customerCode: interval.customerCode || customerCode || 1,
            nOna: interval.nOna || (interval.mode === 6 ? 1 : undefined),
            hasConflict: false,
            conflictWith: [],
            nearZasvetka: false,
            zasvetkaConflict: false,
            zasvetkaDistance: 0,
            willBeSaved: true
        };
    }
}
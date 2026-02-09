import type {
    CreatedProgramData,
    CreateProgramRequest,
    ForecastData,
    Id06OnaDto,
    Id06TsDto,
    OperatorData,
    PpiAssignment,
    TimeInterval,
    WorkMode
} from '$lib/types/schedule';
import { TimeUtils } from '$lib/utils/time';
import type { ScheduleStatus } from '../schedule-creation/types';
import { ScheduleApiService } from './api/scheduleApi.service';
import { ProgramPreparerService } from './data/programPreparer.service';
import { ScheduleConverterService } from './data/scheduleConverter.service';
import { TsIntervalService } from './data/tsInterval.service';
import { AssignmentStatisticsService } from './statistics/assignmentStatistics.service';
import { CityService } from './utils/cities.service';
import { DateFormatterService } from './utils/dateFormatter.service';
import { DurationCalculatorService } from './utils/durationCalculator.service';


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
        scheduleStatus: ScheduleStatus
    ): CreateProgramRequest {
        return ProgramPreparerService.prepareFullProgramData(
            operatorData,
            ppiAssignments,
            createdPrograms,
            selectedDate,
            selectedTime,
            scheduleStatus
        );
    }

    static convertToTimeIntervals(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        workModes: WorkMode[]
    ): TimeInterval[] {
        const intervals: TimeInterval[] = [];
        
        // Обработка КВД
        if (operatorData.kvdList?.length) {
            operatorData.kvdList.forEach(kvd => {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === kvd.id && a.recordType === 'kvd'
                );
                
                if (assignment) {
                    intervals.push(this.createKvdInterval(kvd, assignment, operatorData.main?.kZajv));
                }
            });
        }
        
        // Обработка ТНП
        if (operatorData.tnpList?.length) {
            operatorData.tnpList.forEach(tnp => {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === tnp.id && a.recordType === 'tnp'
                );
                
                if (assignment) {
                    intervals.push(this.createTnpInterval(tnp, assignment, operatorData.main?.kZajv));
                }
            });
        }
        
        // Обработка ТС
        if (operatorData.tsList?.length) {
            operatorData.tsList.forEach(ts => {
                const tsSubIntervals = TsIntervalService.convertTsToSubIntervals(ts, ppiAssignments);
                intervals.push(...tsSubIntervals);
            });
        }

        // Обработка ОНА
        if (operatorData.onaList?.length) {
            operatorData.onaList.forEach(ona => {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === ona.id && a.recordType === 'ona'
                );
                
                if (assignment) {
                    intervals.push(this.createOnaInterval(ona, assignment, operatorData.main?.kZajv));
                }
            });
        }
        
        return intervals.map(interval => this.applyDefaultIntervalValues(interval, operatorData.main?.kZajv));
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
        return DurationCalculatorService.calculateDuration(startStr, endStr);
    }

    static getAssignmentStatistics(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[]
    ) {
        return AssignmentStatisticsService.getAssignmentStatistics(operatorData, ppiAssignments);
    }

    static formatDateTime(dateStr: string): string {
        return DateFormatterService.formatDateTime(dateStr);
    }

    static formatTimeOnly(dateStr: string): string {
        return DateFormatterService.formatTimeOnly(dateStr);
    }

    static convertTsToSubIntervals(
        tsRecord: Id06TsDto,
        ppiAssignments: PpiAssignment[]
    ): TimeInterval[] {
        return TsIntervalService.convertTsToSubIntervals(tsRecord, ppiAssignments);
    }

    static getDefaultMsuConfig() {
        return ScheduleConverterService.getDefaultMsuConfig();
    }

    static formatTimeFromISO(isoString: string): string {
        return ScheduleConverterService.formatTimeFromISO(isoString);
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

    private static createKvdInterval(kvd: any, assignment: PpiAssignment, customerCode?: number): TimeInterval {
        const date = kvd.dn.split('T')[0];

        return {
            id: `kvd_${kvd.id}`,
            mode: 7,
            date: date,
            startTime: TimeUtils.extractTimeFromTimestamp(kvd.dn), 
            endTime: TimeUtils.extractTimeFromTimestamp(kvd.dk),
            city: this.getCityByPpi(assignment.ppiNum),
            color: this.getColorByPpi(assignment.ppiNum),
            title: `Калибровка ВД (ППИ ${assignment.ppiNum})`,
            description: `Калибровка ВД, ID: ${kvd.id}`,
            ppi: assignment.ppiNum,
            dlit: this.calculateDuration(kvd.dn, kvd.dk),
            customerCode: customerCode || 5,
            hasConflict: false,
            conflictWith: [],
            nearZasvetka: false,
            zasvetkaConflict: false,
            zasvetkaDistance: 0,
            willBeSaved: true
        };
    }

    private static createTnpInterval(tnp: any, assignment: PpiAssignment, customerCode?: number): TimeInterval {
        const date = tnp.dn.split('T')[0];

        return {
            id: `tnp_${tnp.id}`,
            mode: 4,
            date: date,
            startTime: TimeUtils.extractTimeFromTimestamp(tnp.dn), 
            endTime: TimeUtils.extractTimeFromTimestamp(tnp.dk), 
            city: this.getCityByPpi(assignment.ppiNum),
            color: this.getColorByPpi(assignment.ppiNum),
            title: `ТНП (ППИ ${assignment.ppiNum})`,
            description: `Режим ТНП, длительность: ${tnp.dlit} сек`,
            ppi: assignment.ppiNum,
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
        assignment: PpiAssignment,
        customerCode?: number
    ): TimeInterval {
        const date = ona.dn.split('T')[0];

        return {
            id: `ona_${ona.id}`,
            mode: 6,  
            date: date,
            startTime: TimeUtils.extractTimeFromTimestamp(ona.dn),
            endTime: TimeUtils.extractTimeFromTimestamp(ona.dk),
            city: this.getCityByPpi(assignment.ppiNum),
            color: this.getColorByPpi(assignment.ppiNum),
            title: `Юстировка ОНА (Антенна ${ona.nOna})`,
            description: `Юстировка ОНА, длительность: ${ona.dlit} сек`,
            ppi: assignment.ppiNum,
            dlit: ona.dlit,
            nOna: ona.nOna,  
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
import type {
    CreatedProgramData,
    CreateProgramRequest,
    OperatorData,
    PpiAssignment,
    ProgramModeData,
} from '$lib/types/schedule';
import type { ScheduleStatus } from '../../schedule-creation/types';
import { DurationCalculatorService } from '../utils/durationCalculator.service';

export class ProgramPreparerService {
    static prepareProgramData(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        selectedDate: string,
        selectedTime: string,
        scheduleStatus: ScheduleStatus
    ): CreateProgramRequest {
        const numRp = this.generateProgramNumber();
        const numKa = operatorData.main.nKa;

        const mainData = {
            numRp,
            numKa,
            dateOn: `${selectedDate}T${selectedTime}:00`,
            dateOff: this.calculateDateOff(operatorData, selectedDate, selectedTime),
            typeRp: scheduleStatus === 'main' ? 3 : 5,
            prOtpr: 0
        };

        const modes: ProgramModeData[] = [];

        // Добавляем режимы КВД
        if (operatorData.kvdList?.length) {
            operatorData.kvdList.forEach(kvd => {
                const assignment = ppiAssignments.find(a => a.recordId === kvd.id && a.recordType === 'kvd');
                if (assignment) {
                    modes.push(this.createKvdModeData(numRp, numKa, kvd, assignment));
                }
            });
        }

        // Добавляем режимы ТНП
        if (operatorData.tnpList?.length) {
            operatorData.tnpList.forEach(tnp => {
                const assignment = ppiAssignments.find(a => a.recordId === tnp.id && a.recordType === 'tnp');
                if (assignment) {
                    modes.push(this.createTnpModeData(numRp, numKa, tnp, assignment));
                }
            });
        }

        // Добавляем режимы ТС
        if (operatorData.tsList?.length) {
            operatorData.tsList.forEach(ts => {
                const assignment = ppiAssignments.find(a => a.recordId === ts.id && a.recordType === 'ts');
                if (assignment) {
                    modes.push(this.createTsModeData(numRp, numKa, ts, assignment, operatorData.main?.kZajv));
                }
            });
        }

        console.log("Подготовленная ПРЦА: ", mainData);
        console.log("Подготовленные записи режимов ПРЦА: ", modes);

        return { mainData, modes };
    }

    static prepareFullProgramData(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        createdPrograms: CreatedProgramData[],
        selectedDate: string,
        selectedTime: string,
        scheduleStatus: ScheduleStatus
    ): CreateProgramRequest {
        const baseRequest = this.prepareProgramData(
            operatorData,
            ppiAssignments,
            selectedDate,
            selectedTime,
            scheduleStatus
        );
        
        createdPrograms.forEach(created => {
            baseRequest.modes.push(created.modeData);
        });
        
        return baseRequest;
    }

    private static createKvdModeData(numRp: number, numKa: number, kvd: any, assignment: PpiAssignment): ProgramModeData {
        return {
            numRp,
            numKa,
            dateOn: kvd.dn,
            dateOff: kvd.dk,
            kodMode: 7,
            numPpi: assignment.ppiNum,
            dlit: DurationCalculatorService.calculateDuration(kvd.dn, kvd.dk),
            kvdData: {
                id: kvd.id,
                idMain: kvd.idMain,
                dn: kvd.dn,
                dk: kvd.dk,
                prMsu: kvd.prMsu,
                prBssd: kvd.prBssd,
                prZg: kvd.prZg
            }
        };
    }

    private static createTnpModeData(numRp: number, numKa: number, tnp: any, assignment: PpiAssignment): ProgramModeData {
        return {
            numRp,
            numKa,
            dateOn: tnp.dn,
            dateOff: tnp.dk,
            kodMode: 4,
            numPpi: assignment.ppiNum,
            dlit: tnp.dlit,
            tnpData: {
                id: tnp.id,
                idMain: tnp.idMain,
                dn: tnp.dn,
                dk: tnp.dk,
                dlit: tnp.dlit,
            }
        };
    }

    private static createTsModeData(numRp: number, numKa: number, ts: any, assignment: PpiAssignment, customerCode?: number): ProgramModeData {
        return {
            numRp,
            numKa,
            dateOn: ts.dn,
            dateOff: ts.dk,
            kodMode: 8,
            numPpi: assignment.ppiNum,
            dlit: DurationCalculatorService.calculateDuration(ts.dn, ts.dk),
            zakazchik: this.getCustomerLabel(customerCode || 5),
            tsData: {
                id: ts.id,
                idMain: ts.idMain,
                dn: ts.dn,
                dk: ts.dk,
                tip: ts.tip,
                reg: ts.reg,
                prMsu1: ts.prMsu1,
                prVdMsu1: ts.prVdMsu1,
                prIkMsu1: ts.prIkMsu1,
                prVd1_1: ts.prVd1_1,
                prVd2_1: ts.prVd2_1,
                prVd3_1: ts.prVd3_1,
                prIk4_1: ts.prIk4_1,
                prIk5_1: ts.prIk5_1,
                prIk6_1: ts.prIk6_1,
                prIk7_1: ts.prIk7_1,
                prIk8_1: ts.prIk8_1,
                prIk9_1: ts.prIk9_1,
                prIk10_1: ts.prIk10_1,
                prMsu2: ts.prMsu2,
                prVdMsu2: ts.prVdMsu2,
                prIkMsu2: ts.prIkMsu2,
                prVd1_2: ts.prVd1_2,
                prVd2_2: ts.prVd2_2,
                prVd3_2: ts.prVd3_2,
                prIk4_2: ts.prIk4_2,
                prIk5_2: ts.prIk5_2,
                prIk6_2: ts.prIk6_2,
                prIk7_2: ts.prIk7_2,
                prIk8_2: ts.prIk8_2,
                prIk9_2: ts.prIk9_2,
                prIk10_2: ts.prIk10_2,
                prOtklZg: ts.prOtklZg
            }
        };
    }

    public static calculateDateOff(
        operatorData: OperatorData,
        selectedDate: string,
        selectedTime: string
    ): string {
        let latestDate = new Date(`${selectedDate}T${selectedTime}:00`);

        const allRecords = [
            ...(operatorData.kvdList || []),
            ...(operatorData.tnpList || []),
            ...(operatorData.tsList || [])
        ];

        allRecords.forEach(record => {
            const endDate = new Date(record.dk);
            if (endDate > latestDate) {
                latestDate = endDate;
            }
        });

        return latestDate.toISOString();
    }

    public static generateProgramNumber(): number {
        return Math.floor(Date.now() / 1000);
    }

    public static getCustomerLabel(code: number): string {
        const customerLabels: Record<number, string> = {
            1: 'Заказчик 1',
            2: 'Заказчик 2',
            3: 'Заказчик 3',
            4: 'Заказчик 4',
            5: 'Заказчик 5'
        };
        return customerLabels[code] || 'Неизвестный заказчик';
    }
}
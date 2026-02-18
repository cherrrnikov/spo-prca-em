import type {
    CreatedProgramData,
    CreateProgramRequest,
    OperatorData,
    PpiAssignment,
    ProgramModeData
} from '$lib/types';
import { TimeUtils } from '$lib/utils/time';
import type { ScheduleStatus } from '../../schedule-creation/types';

export class ProgramPreparerService {
    static prepareProgramData(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        selectedDate: string,
        selectedTime: string,
        scheduleStatus: ScheduleStatus
    ): CreateProgramRequest {
        const numRp = this.generateProgramNumber();
        const numKa = operatorData.main.n_ka;

        const mainData = {
            numRp,
            numKa,
            dateOn: `${selectedDate}T${selectedTime}:00`,
            dateOff: this.calculateDateOff(operatorData, selectedDate, selectedTime),
            typeRp: scheduleStatus === 'main' ? 3 : 5,
            prOtpr: 0
        };

        const modes: ProgramModeData[] = [];

        if (operatorData.kvd_list?.length) {
            operatorData.kvd_list.forEach(kvd => {
                const assignment = ppiAssignments.find(a => a.recordId === kvd.id && a.recordType === 'kvd');
                if (assignment) {
                    modes.push(this.createKvdModeData(numRp, numKa, kvd, assignment));
                }
            });
        }

        if (operatorData.tnp_list?.length) {
            operatorData.tnp_list.forEach(tnp => {
                const assignment = ppiAssignments.find(a => a.recordId === tnp.id && a.recordType === 'tnp');
                if (assignment) {
                    modes.push(this.createTnpModeData(numRp, numKa, tnp, assignment));
                }
            });
        }

        if (operatorData.ts_list?.length) {
            operatorData.ts_list.forEach(ts => {
                const assignment = ppiAssignments.find(a => a.recordId === ts.id && a.recordType === 'ts');
                if (assignment) {
                    modes.push(this.createTsModeData(numRp, numKa, ts, assignment, operatorData.main?.k_zajv));
                }
            });
        }

        if (operatorData.ona_list?.length) {
            operatorData.ona_list.forEach(ona => {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === ona.id && a.recordType === 'ona'
                );
                if (assignment) {
                    modes.push(this.createOnaModeData(numRp, numKa, ona, assignment));
                }
            });
        }

        console.log('ПРЦА подготовлена:', {
            main: mainData,
            modesCount: modes.length,
            kvd: modes.filter(m => m.kodMode === 7).length,
            tnp: modes.filter(m => m.kodMode === 4).length,
            ts: modes.filter(m => m.kodMode === 8).length,
            ona: modes.filter(m => m.kodMode === 6).length
        });

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
            dlit: TimeUtils.calculateDuration(kvd.dn, kvd.dk),
            kvdData: {
                id: kvd.id,
                idMain: kvd.idMain,
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
                prMsu: tnp.prMsu,
                prBssd: tnp.prBssd,
                prZg: tnp.prZg
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
            dlit: TimeUtils.calculateDuration(ts.dn, ts.dk),
            zakazchik: this.getCustomerLabel(customerCode || 5),
            tsData: {
                id: ts.id,
                idMain: ts.idMain,
                tip: ts.tip,
                reg: ts.reg,
                dlit: ts.dlit,
                prMsu1: ts.prMsu1,
                vd1Msu1: ts.vd1Msu1,
                vd2Msu1: ts.vd2Msu1,
                vd3Msu1: ts.vd3Msu1,
                ik4Msu1: ts.ik4Msu1,
                ik5Msu1: ts.ik5Msu1,
                ik6Msu1: ts.ik6Msu1,
                ik7Msu1: ts.ik7Msu1,
                ik8Msu1: ts.ik8Msu1,
                ik9Msu1: ts.ik9Msu1,
                ik10Msu1: ts.ik10Msu1,
                prMsu2: ts.prMsu2,
                vd1Msu2: ts.vd1Msu2,
                vd2Msu2: ts.vd2Msu2,
                vd3Msu2: ts.vd3Msu2,
                ik4Msu2: ts.ik4Msu2,
                ik5Msu2: ts.ik5Msu2,
                ik6Msu2: ts.ik6Msu2,
                ik7Msu2: ts.ik7Msu2,
                ik8Msu2: ts.ik8Msu2,
                ik9Msu2: ts.ik9Msu2,
                ik10Msu2: ts.ik10Msu2,
                prBssd: ts.prBssd,
                prZg: ts.prZg,
                prOtklZgBssd: ts.prOtklZgBssd
            }
        };
    }

    private static createOnaModeData(
        numRp: number, 
        numKa: number, 
        ona: any, 
        assignment: PpiAssignment
    ): ProgramModeData {
        return {
            numRp,
            numKa,
            dateOn: ona.dn,
            dateOff: ona.dk,
            kodMode: 6,  
            numPpi: assignment.ppiNum,
            dlit: ona.dlit,
            onaData: {  
                id: ona.id,
                idMain: ona.id_main,  
                typeOmi: ona.typeOmi,
                dN: ona.dn,
                dK: ona.dk,
                nOna: ona.n_ona,
                nPpi: ona.nPpi
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
            ...(operatorData.kvd_list || []),
            ...(operatorData.tnp_list || []),
            ...(operatorData.ts_list || []),
            ...(operatorData.ona_list || [])
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
import { DEFAULT_SHOOTING_DURATION } from '$lib/config/schedule.config';
import { RECORD_TYPES } from '$lib/constants/recordTypes';
import { CUSTOMER_CODES } from '$lib/constants/schedule';
import type {
    CreatedProgramData,
    Id02Dto,
    OperatorData,
    PpiAssignment,
    ProgramModeData,
    TimeInterval
} from '$lib/types';
import { ModeUtils } from '$lib/utils/mode';
import { TimeUtils } from '$lib/utils/time';

/**
 * Сервис формирования CreatedProgramData из данных оператора (ИД06).
 * Каждый интервал из ИД06 превращается в запись для последующего сохранения ПРЦА.
 */
export class ProgramCreatorService {

    static createProgramsFromOperatorData(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        date: string,
        intervals: TimeInterval[],
        bortData: Id02Dto | null
    ): CreatedProgramData[] {
        const programs: CreatedProgramData[] = [];
        const mainId = operatorData.main.id;
        const numKa = operatorData.main.n_ka;

        this.processKvd(programs, operatorData, ppiAssignments, intervals, mainId, numKa);
        this.processTnp(programs, operatorData, ppiAssignments, intervals, mainId, numKa);
        this.processTs(programs, operatorData, ppiAssignments, intervals, mainId, numKa, date, bortData);
        this.processOna(programs, operatorData, ppiAssignments, intervals, mainId, numKa);

        return programs;
    }

    private static processKvd(
        programs: CreatedProgramData[],
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        intervals: TimeInterval[],
        mainId: number,
        numKa: number
    ): void {
        if (!operatorData.kvd_list) return;

        operatorData.kvd_list.forEach((kvd: any) => {
            const assignment = ppiAssignments.find(a =>
                a.recordId === kvd.id && a.recordType === RECORD_TYPES.KVD
            );
            if (!assignment) return;

            const modeData: ProgramModeData = {
                numRp: 0,
                numKa,
                dateOn: kvd.dn,
                dateOff: kvd.dk,
                kodMode: 7,
                numPpi: assignment.ppiNum,
                dlit: TimeUtils.calculateDuration(kvd.dn, kvd.dk),
                zakazchik: ModeUtils.getCustomerLabel(CUSTOMER_CODES, 1),
                kvdData: {
                    id: kvd.id,
                    idMain: mainId,
                    prMsu: kvd.pr_msu,
                    prBssd: kvd.pr_bssd,
                    prZg: kvd.pr_zg
                }
            };

            const timeInterval = intervals.find(i => i.id === `kvd_${kvd.id}`);
            if (timeInterval) {
                timeInterval.customerCode = 1;
                timeInterval.kvdConfig = {
                    prMsu: kvd.pr_msu,
                    prBssd: kvd.pr_bssd,
                    prZg: kvd.pr_zg
                };
                programs.push({ tempId: `kvd_${kvd.id}`, modeData, timeInterval });
            }
        });
    }

    private static processTnp(
        programs: CreatedProgramData[],
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        intervals: TimeInterval[],
        mainId: number,
        numKa: number
    ): void {
        if (!operatorData.tnp_list) return;

        operatorData.tnp_list.forEach((tnp: any) => {
            const assignment = ppiAssignments.find(a =>
                a.recordId === tnp.id && a.recordType === RECORD_TYPES.TNP
            );
            if (!assignment) return;

            const modeData: ProgramModeData = {
                numRp: 0,
                numKa,
                dateOn: tnp.dn,
                dateOff: tnp.dk,
                kodMode: 4,
                numPpi: assignment.ppiNum,
                dlit: tnp.dlit,
                zakazchik: ModeUtils.getCustomerLabel(CUSTOMER_CODES, 1),
                tnpData: {
                    id: tnp.id,
                    idMain: mainId,
                    prMsu: 1,
                    prBssd: 1,
                    prZg: 1
                }
            };

            const timeInterval = intervals.find(i => i.id === `tnp_${tnp.id}`);
            if (timeInterval) {
                timeInterval.customerCode = 1;
                programs.push({ tempId: `tnp_${tnp.id}`, modeData, timeInterval });
            }
        });
    }

    private static processTs(
        programs: CreatedProgramData[],
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        intervals: TimeInterval[],
        mainId: number,
        numKa: number,
        date: string,
        bortData: Id02Dto | null
    ): void {
        if (!operatorData.ts_list) return;

        operatorData.ts_list.forEach((ts: any) => {
            const assignment = ppiAssignments.find(a =>
                a.recordId === ts.id && a.recordType === RECORD_TYPES.TS
            );
            if (!assignment) return;

            const tsSubIntervals = intervals.filter(i =>
                i.id.startsWith(`ts_${ts.id}`)
            );

            tsSubIntervals.forEach((subInterval, idx) => {
                subInterval.customerCode = 1;

                const tsData = {
                    id: ts.id,
                    idMain: mainId,
                    tip: ts.tip ?? 1,
                    reg: ts.reg ?? 0,
                    dlit: subInterval.dlit || DEFAULT_SHOOTING_DURATION,
                    prMsu1: ts.pr_msu1,
                    vd1Msu1: ts.pr_vd1_1,
                    vd2Msu1: ts.pr_vd2_1,
                    vd3Msu1: ts.pr_vd3_1,
                    ik4Msu1: ts.pr_ik4_1,
                    ik5Msu1: ts.pr_ik5_1,
                    ik6Msu1: ts.pr_ik6_1,
                    ik7Msu1: ts.pr_ik7_1,
                    ik8Msu1: ts.pr_ik8_1,
                    ik9Msu1: ts.pr_ik9_1,
                    ik10Msu1: ts.pr_ik10_1,
                    prMsu2: ts.pr_msu2,
                    vd1Msu2: ts.pr_vd1_2,
                    vd2Msu2: ts.pr_vd2_2,
                    vd3Msu2: ts.pr_vd3_2,
                    ik4Msu2: ts.pr_ik4_2,
                    ik5Msu2: ts.pr_ik5_2,
                    ik6Msu2: ts.pr_ik6_2,
                    ik7Msu2: ts.pr_ik7_2,
                    ik8Msu2: ts.pr_ik8_2,
                    ik9Msu2: ts.pr_ik9_2,
                    ik10Msu2: ts.pr_ik10_2,
                    prBssd: bortData?.pr_bssd ?? 0,
                    prZg: bortData?.pr_zg ?? 0,
                    prOtklZgBssd: ts.pr_otkl_zg
                };

                subInterval.tsData = tsData;

                const modeData: ProgramModeData = {
                    numRp: 0,
                    numKa,
                    dateOn: `${date}T${subInterval.startTime}`,
                    dateOff: `${date}T${subInterval.endTime}`,
                    kodMode: 8,
                    numPpi: assignment.ppiNum,
                    dlit: subInterval.dlit || DEFAULT_SHOOTING_DURATION,
                    zakazchik: ModeUtils.getCustomerLabel(CUSTOMER_CODES, 1),
                    tsData
                };

                programs.push({
                    tempId: `ts_${ts.id}_${idx}`,
                    modeData,
                    timeInterval: subInterval
                });
            });
        });
    }

    private static processOna(
        programs: CreatedProgramData[],
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        intervals: TimeInterval[],
        mainId: number,
        numKa: number
    ): void {
        if (!operatorData.ona_list) return;

        operatorData.ona_list.forEach((ona: any) => {
            const assignment = ppiAssignments.find(a =>
                a.recordId === ona.id && a.recordType === RECORD_TYPES.ONA
            );
            if (!assignment) return;

            const modeData: ProgramModeData = {
                numRp: 0,
                numKa,
                dateOn: ona.dn,
                dateOff: ona.dk,
                kodMode: 6,
                numPpi: assignment.ppiNum,
                dlit: ona.dlit,
                zakazchik: ModeUtils.getCustomerLabel(CUSTOMER_CODES, 1),
                onaData: {
                    id: ona.id,
                    idMain: ona.id_main,
                    typeOmi: 1,
                    dN: ona.dn,
                    dK: ona.dk,
                    nOna: ona.n_ona,
                    nPpi: assignment.ppiNum
                }
            };

            const timeInterval = intervals.find(i => i.id === `ona_${ona.id}`);
            if (timeInterval) {
                timeInterval.customerCode = 1;
                programs.push({ tempId: `ona_${ona.id}`, modeData, timeInterval });
            }
        });
    }
}
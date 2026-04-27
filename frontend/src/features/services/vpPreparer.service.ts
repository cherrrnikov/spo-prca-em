import { MODE_CODES } from '$lib/constants/schedule';
import type { CreatedProgramData, VpCreateRequest, VpKvdData, VpMsuData, VpOmiData, VpOnaData, VpTnpData } from '$lib/types';

export class VpPreparerService {

    static prepareVpData(
        createdPrograms: CreatedProgramData[],
        numKa: number,
        numRp: number,
        dtNRp: string,
        dtKRp: string
    ): VpCreateRequest {

        // Берём только сохраняемые интервалы (без конфликтных)
        const savedPrograms = createdPrograms.filter(p => p.timeInterval.willBeSaved === true);

        const msuList: VpMsuData[] = [];
        const kvdList: VpKvdData[] = [];
        const tnpList: VpTnpData[] = [];
        const omiList: VpOmiData[] = [];
        const onaList: VpOnaData[] = [];

        for (const program of savedPrograms) {
            const mode = program.modeData;

            switch (mode.kodMode) {
                case MODE_CODES.SHOOTING: // обычная съёмка
                case MODE_CODES.TS: // технологическая съёмка
                    if (mode.msuData) {
                        msuList.push(this.createMsuData(program));
                    }
                    break;

                case MODE_CODES.KVD: // КВД
                    if (mode.kvdData) {
                        kvdList.push(this.createKvdData(program));
                    }
                    break;

                case MODE_CODES.TNP: // ТНП
                    tnpList.push(this.createTnpData(program));
                    break;

                case MODE_CODES.OMI: // ОМИ
                    if (mode.omiData) {
                        omiList.push(this.createOmiData(program));
                    }
                    break;

                case MODE_CODES.ONA: // юстировка ОНА
                    if (mode.onaData) {
                        onaList.push(this.createOnaData(program));
                    }
                    break;
            }
        }

        return {
            mainData: {
                numKa,
                numRp,
                dtNRp,
                dtKRp
            },
            msuList,
            kvdList,
            tnpList,
            omiList,
            onaList
        };
    }

    private static createMsuData(program: CreatedProgramData): VpMsuData {
        const ts = program.modeData.msuData!;
        const interval = program.timeInterval;
        const date = interval.date;

        return {
            kodReg: ts.reg,
            dateNach: `${date}T${interval.startTime}`,
            dateCon: `${date}T${interval.endTime}`,
            complectMsu1: ts.prMsu1,
            vd11: ts.vd1Msu1,
            vd12: ts.vd2Msu1,
            vd13: ts.vd3Msu1,
            ik14: ts.ik4Msu1,
            ik15: ts.ik5Msu1,
            ik16: ts.ik6Msu1,
            ik17: ts.ik7Msu1,
            ik18: ts.ik8Msu1,
            ik19: ts.ik9Msu1,
            ik110: ts.ik10Msu1,
            complectMsu2: ts.prMsu2,
            vd21: ts.vd1Msu2,
            vd22: ts.vd2Msu2,
            vd23: ts.vd3Msu2,
            ik24: ts.ik4Msu2,
            ik25: ts.ik5Msu2,
            ik26: ts.ik6Msu2,
            ik27: ts.ik7Msu2,
            ik28: ts.ik8Msu2,
            ik29: ts.ik9Msu2,
            ik210: ts.ik10Msu2,
            tip: ts.tip,
            numPpi: program.modeData.numPpi,
            dlit: interval.dlit || ts.dlit,
            durationCycle: ts.tip === 1 ? 1800 : 900
        };
    }

    private static createKvdData(program: CreatedProgramData): VpKvdData {
        const kvd = program.modeData.kvdData!;
        return {
            dateNach: program.modeData.dateOn,
            dateCon: program.modeData.dateOff,
            complectMsu: kvd.prMsu,
            numPpi: program.modeData.numPpi,
            dlit: program.modeData.dlit
        };
    }

    private static createTnpData(program: CreatedProgramData): VpTnpData {
        return {
            dateNach: program.modeData.dateOn,
            dateCon: program.modeData.dateOff,
            numPpi: program.modeData.numPpi,
            dlit: program.modeData.dlit
        };
    }

    private static createOmiData(program: CreatedProgramData): VpOmiData {
        const omi = program.modeData.omiData!;
        return {
            typeOmi: omi.typeOmi,
            dateNach: program.modeData.dateOn,
            dateCon: program.modeData.dateOff,
            numPpi: program.modeData.numPpi,
            dlit: program.modeData.dlit
        };
    }

    private static createOnaData(program: CreatedProgramData): VpOnaData {
        const ona = program.modeData.onaData!;
        return {
            numUstOna: ona.nOna,
            dateNach: program.modeData.dateOn,
            dateCon: program.modeData.dateOff,
            numPpi: program.modeData.numPpi,
            dlit: program.modeData.dlit
        };
    }
}
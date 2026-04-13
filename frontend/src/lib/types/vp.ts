export interface VpCreateRequest {
    mainData: {
        numKa: number;
        numRp: number;
        rnf?: number;
        dsf?: string;
        dtNRp: string;
        dtKRp: string;
    };
    msuList: VpMsuData[];
    kvdList: VpKvdData[];
    tnpList: VpTnpData[];
    omiList: VpOmiData[];
    onaList: VpOnaData[];
}

export interface VpMsuData {
    kodReg: number;
    numMsu?: number;
    dateNach: string;
    dateCon: string;
    complectMsu1: number;
    vd11: number;
    vd12: number;
    vd13: number;
    ik14: number;
    ik15: number;
    ik16: number;
    ik17: number;
    ik18: number;
    ik19: number;
    ik110: number;
    complectMsu2: number;
    vd21: number;
    vd22: number;
    vd23: number;
    ik24: number;
    ik25: number;
    ik26: number;
    ik27: number;
    ik28: number;
    ik29: number;
    ik210: number;
    tip: number;
    numPpi: number;
    dlit: number;
    durationCycle?: number;
}

export interface VpKvdData {
    numKvd?: number;
    dateNach: string;
    dateCon: string;
    complectMsu: number;
    numPpi: number;
    dlit: number;
}

export interface VpTnpData {
    numTnp?: number;
    dateNach: string;
    dateCon: string;
    numPpi: number;
    dlit: number;
}

export interface VpOmiData {
    numOmi?: number;
    typeOmi: number;
    dateNach: string;
    dateCon: string;
    numPpi: number;
    dlit: number;
}

export interface VpOnaData {
    numUstOna: number;
    dateNach: string;
    dateCon: string;
    numPpi: number;
    dlit: number;
}
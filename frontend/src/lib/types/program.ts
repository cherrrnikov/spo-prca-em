import type { RecordType } from "$lib/constants/recordTypes";
import type { TimeInterval } from "./interval";

export interface CreateProgramRequest {
    mainData: {
        numRp: number;
        numKa: number;
        dateOn: string;
        dateOff: string;
        typeRp: number;
        prOtpr: number;
    };
    modes: ProgramModeData[];
}

export interface ProgramModeData {
    numRp: number;
    numKa: number;
    dateOn: string;
    dateOff: string;
    kodMode: number;
    numPpi: number;
    dlit: number;
    zakazchik?: string;
    
    kvdData?: {
        id: number;
        idMain: number;
        prMsu: number;
        prBssd: number;
        prZg: number;
    };
    
    tnpData?: {
        id: number;
        idMain: number;
        prMsu: number;
        prBssd: number;
        prZg: number;
    };
    
    tsData?: {
        id: number;
        idMain: number;
        tip: number;
        reg: number;
        dlit: number;
        prMsu1: number;
        vd1Msu1: number;
        vd2Msu1: number;
        vd3Msu1: number;
        ik4Msu1: number;
        ik5Msu1: number;
        ik6Msu1: number;
        ik7Msu1: number;
        ik8Msu1: number;
        ik9Msu1: number;
        ik10Msu1: number;
        prMsu2: number;
        vd1Msu2: number;
        vd2Msu2: number;
        vd3Msu2: number;
        ik4Msu2: number;
        ik5Msu2: number;
        ik6Msu2: number;
        ik7Msu2: number;
        ik8Msu2: number;
        ik9Msu2: number;
        ik10Msu2: number;
        prBssd: number;
        prZg: number;
        prOtklZgBssd: number;
    };

    onaData?: {  
        id: number;
        idMain: number;
        typeOmi: number;
        dN: string;
        dK: string;
        nOna: number;
        nPpi: number;
    };

    omiData?: {
        id: number;
        idMain: number;
        numOmi: number;    
        typeOmi: number;   
        dateNach: string; 
        dateCon: string;  
        dlit: number;      
    };
}

export interface PpiAssignment {
    recordId: number;
    recordType: RecordType;
    ppiId: number;
    ppiNum: number;
}

export interface CreatedProgramData {
    tempId: string;
    modeData: ProgramModeData;
    timeInterval: TimeInterval; // TimeInterval будет импортироваться из interval.ts
}
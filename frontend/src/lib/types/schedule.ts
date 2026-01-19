export interface TimeInterval {
    id: string;
    mode: string;
    startTime: string;
    endTime: string;
    city: string;
    color: string;
    title?: string;
    description?: string;
}

export interface WorkMode {
    id: string;
    label: string;
    order: string;
}

export interface City {
    id: string;
    name: string;
    color: string;
}

export interface Ppi {
    id: number;
    name: string;
    // code: string;
    numPpi: number;
}

export interface Id06MainDto {
    id: number;
    nKa: number;
    dNp: string;
    dataZap: string;
    rnf: number;
    nSp: number;
    dsf: string;
    kZajv: number;
    nFormId: number;
    used: number;
}

export interface Id06KvdDto {
    id: number;
    idMain: number;
    dn: string;
    dk: string;
    prMsu: number;
    prBssd: number;
    prZg: number;
}

export interface Id06TnpDto {
    id: number;
    idMain: number;
    dn: string;
    dk: string;
    dlit: number;
}

export interface Id06TsDto {
    id: number;
    idMain: number;
    dn: string;
    dk: string;
    tip: number;
    reg: number;
    prMsu1: number;
    prVdMsu1: number
    prIkMsu1: number;
    prVd1_1: number;
    prVd2_1: number;
    prVd3_1: number;
    prIk4_1: number;
    prIk5_1: number;
    prIk6_1: number;
    prIk7_1: number;
    prIk8_1: number;
    prIk9_1: number;
    prIk10_1: number;
    prMsu2: number;
    prVdMsu2: number;
    prIkMsu2: number;
    prVd1_2: number;
    prVd2_2: number;
    prVd3_2: number;
    prIk4_2: number;
    prIk5_2: number;
    prIk6_2: number;
    prIk7_2: number;
    prIk8_2: number;
    prIk9_2: number;
    prIk10_2: number;
    prOtklZg: number;
}

export interface OperatorData {
    main: Id06MainDto;
    kvdList: Id06KvdDto[];
    tnpList: Id06TnpDto[];
    tsList: Id06TsDto[];
    totalIntervals: number;
}

export interface PpiSelectionModal {
    isOpen: boolean;
    currentRecord: Id06KvdDto | Id06TnpDto | Id06TsDto | null;
    recordType: 'kvd' | 'tnp' | 'ts' | null;
    recordIndex: number;
    totalRecords: number;
    selectedPpi: Ppi | null;
    recordTitle: string;
}

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
        dn: string;
        dk: string;
        prMsu: number;
        prBssd: number;
        prZg: number;
    };
    
    tnpData?: {
        id: number;
        idMain: number;
        dn: string;
        dk: string;
        dlit: number;
    };
    
    tsData?: {
        id: number;
        idMain: number;
        dn: string;
        dk: string;
        tip: number;
        reg: number;
        prMsu1: number;
        prVdMsu1: number
        prIkMsu1: number;
        prVd1_1: number;
        prVd2_1: number;
        prVd3_1: number;
        prIk4_1: number;
        prIk5_1: number;
        prIk6_1: number;
        prIk7_1: number;
        prIk8_1: number;
        prIk9_1: number;
        prIk10_1: number;
        prMsu2: number;
        prVdMsu2: number;
        prIkMsu2: number;
        prVd1_2: number;
        prVd2_2: number;
        prVd3_2: number;
        prIk4_2: number;
        prIk5_2: number;
        prIk6_2: number;
        prIk7_2: number;
        prIk8_2: number;
        prIk9_2: number;
        prIk10_2: number;
        prOtklZg: number;
    };
}

export interface PpiAssignment {
    recordId: number;
    recordType: 'kvd' | 'tnp' | 'ts';
    ppiId: number;
    ppiNum: number;
}

export interface ProgramCreationState {
    step: 'form' | 'ppi_selection' | 'review' | 'saving';
    currentRecordType: 'kvd' | 'tnp' | 'ts' | null;
    currentRecordIndex: number;
    processedRecords: number;
    totalRecords: number;
}
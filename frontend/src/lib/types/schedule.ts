export interface TimeInterval {
    id: string;
    mode: number;
    date: string,
    startTime: string;
    endTime: string;
    city: string;
    color: string;
    title?: string;
    description?: string;
    ppi?: number;
    dlit?: number;
    customerCode?: number;

    kvdConfig?: {
        prMsu: number; // 0-МСУ1, 1-МСУ2
        prBssd: number; // 0-БССД1, 1-БССД2  
        prZg: number; // 0-ЗГ1, 1-ЗГ2, 2-ЗГ3, 3-ЗГ4
    };
    
    // msu1Vd?: number[];
    // msu2Vd?: number[];
    
    msu1Config?: TsMsuConfig;
    msu2Config?: TsMsuConfig;
    
    hasConflict?: boolean;  // true если есть пересечение с интервалом другого режима
    conflictWith?: number[]; // массив mode id с которыми есть пересечение
    willBeSaved?: boolean;  // true - будет сохранено, false - не будет сохранено
    nearZasvetka?: boolean;          // true если интервал близко к засветке (< 60 секунд)
    zasvetkaConflict?: boolean;      // true если пересекается с засветкой
    zasvetkaDistance?: number;       // расстояние до ближайшей засветки в секундах
}

export interface CustomerCode {
    value: number;
    label: string;
}

export interface WorkMode {
    id: number;
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
    numPpi: number;
}

export interface ZgOption {
    value: number;
    label: string;
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

// export interface ProgramCreationState {
//     step: 'form' | 'ppi_selection' | 'review' | 'saving';
//     currentRecordType: 'kvd' | 'tnp' | 'ts' | null;
//     currentRecordIndex: number;
//     processedRecords: number;
//     totalRecords: number;
// }

export interface ModeCreationForm {
    modeType: number | null;
    ppiNum: number;
    duration: number;
    customerCode: number;
    startTime: string;
    kvdConfig: {
        prMsu: number; // 0-МСУ1, 1-МСУ2
        prBssd: number; // 0-БССД1, 1-БССД2  
        prZg: number; // 0-ЗГ1, 1-ЗГ2, 2-ЗГ3, 3-ЗГ4
    };
    // msu1Vd: number[];
    // msu2Vd: number[];
    msu1Config: TsMsuConfig;
    msu2Config: TsMsuConfig;
}

export interface TsMsuConfig {
    prMsu: number;
    prVdMsu: number;
    prIkMsu: number;
    vd1: number;
    vd2: number;
    vd3: number;
    ik4: number;
    ik5: number;
    ik6: number;
    ik7: number;
    ik8: number;
    ik9: number;
    ik10: number;
}

export interface CreatedProgramData {
    tempId: string; // временный id для отображения
    modeData: ProgramModeData;
    timeInterval: TimeInterval; 
}

export interface ScheduleCreationState {
    step: 'form' | 'ppi_selection' | 'review' | 'saving';
    currentRecordType: 'kvd' | 'tnp' | 'ts' | null;
    currentRecordIndex: number;
    processedRecords: number;
    totalRecords: number;
    createdIntervals?: CreatedProgramData[];
}

export interface ForecastDto {
    id: number;
    dn: string;
    dk: string;
    nKa: number;
    nInit: number;
}

export interface ShadowDto {
    id: number;
    nRec: number;
    dTIn: string;
    dTOut: string;
    duration: number;
}

export interface ZasvetkaDto {
    id: number;
    nRec: number;
    dTIn: string;
    dTOut: string;
    duration: number;
}

export interface ForecastDataResponse {
    forecast: ForecastDto;
    shadows: ShadowDto[];
    zasvetki: ZasvetkaDto[];
}

export interface ForecastData {
    main: ForecastDto;
    shadows: ShadowDto[];
    zasvetki: ZasvetkaDto[];
    totalIntervals: number; // shadows.length + zasvetki.length
}

export interface ShadowInterval {
    id: string;
    type: 'shadow';
    startTime: string;
    endTime: string;
    duration: number;
    title: string;
    color: string;
    opacity: number;
    zIndex: number;
}

export interface ZasvetkaInterval {
    id: string;
    type: 'zasvetka';
    startTime: string;
    endTime: string;
    duration: number;
    title: string;
    color: string;
    opacity: number;
    zIndex: number;
}
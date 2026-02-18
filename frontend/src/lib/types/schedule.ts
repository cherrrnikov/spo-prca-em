import type { ConstraintViolation } from "./constraints";

export interface TimeInterval {
    id: string;
    mode: number;
    date: string,
    startTime: string;
    endTime: string;
    city?: string;
    color: string;
    title?: string;
    description?: string;
    ppi?: number;
    dlit?: number;
    customerCode?: number;
    nOna?: number;

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
    inShadow?: boolean,
    willBeSavedInShadow?: boolean,
    shadowPriority?: number,
    zasvetkaConflict?: boolean;      // true если пересекается с засветкой
    zasvetkaDistance?: number;       // расстояние до ближайшей засветки в секундах
    isAstrocorrection?: boolean;
    constraintViolations?: ConstraintViolation[];
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
    n_ka: number;      
    d_np: string;  
    data_zap: string;  
    rnf: number;
    n_sp: number;        
    dsf: string;
    k_zajv: number;      
    n_form_id: number; 
    used: number;
}

export interface Id06KvdDto {
    id: number; 
    id_main: number;  
    dn: string;
    dk: string;
    pr_msu: number;    
    pr_bssd: number;    
    pr_zg: number;     
}

export interface Id06OnaDto {
    id: number;
    id_main: number;     
    dn: string;
    dk: string;
    n_ona: number;       
    dlit: number;
}

export interface Id06TnpDto {
    id: number;
    id_main: number;     
    dn: string;
    dk: string;
    dlit: number;
}

export interface Id06TsDto {
    id: number;
    id_main: number;
    dn: string;
    dk: string;
    tip: number;
    reg: number;
    pr_msu1: number;
    pr_vd_msu1: number;
    pr_ik_msu1: number;
    pr_vd1_1: number;       
    pr_vd2_1: number;      // было prVd2_1
    pr_vd3_1: number;      // было prVd3_1
    pr_ik4_1: number;      // было prIk4_1
    pr_ik5_1: number;      // было prIk5_1
    pr_ik6_1: number;      // было prIk6_1
    pr_ik7_1: number;      // было prIk7_1
    pr_ik8_1: number;      // было prIk8_1
    pr_ik9_1: number;      // было prIk9_1
    pr_ik10_1: number;     // было prIk10_1
    pr_msu2: number;       // было prMsu2
    pr_vd_msu2: number;    // было prVdMsu2
    pr_ik_msu2: number;    // было prIkMsu2
    pr_vd1_2: number;      // было prVd1_2
    pr_vd2_2: number;      // было prVd2_2
    pr_vd3_2: number;      // было prVd3_2
    pr_ik4_2: number;      // было prIk4_2
    pr_ik5_2: number;      // было prIk5_2
    pr_ik6_2: number;      // было prIk6_2
    pr_ik7_2: number;      // было prIk7_2
    pr_ik8_2: number;      // было prIk8_2
    pr_ik9_2: number;      // было prIk9_2
    pr_ik10_2: number;     // было prIk10_2
    pr_otkl_zg: number;    // было prOtklZg
}

export interface OperatorData {
    main: Id06MainDto;
    kvd_list: Id06KvdDto[];
    tnp_list: Id06TnpDto[];
    ts_list: Id06TsDto[];
    ona_list: Id06OnaDto[];
    total_intervals: number;
}

export interface PpiSelectionModal {
    isOpen: boolean;
    currentRecord: Id06KvdDto | Id06TnpDto | Id06TsDto | Id06OnaDto | null;
    recordType: 'kvd' | 'tnp' | 'ts' | 'ona' | null;
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
}

export interface PpiAssignment {
    recordId: number;
    recordType: 'kvd' | 'tnp' | 'ts' | 'ona';
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
    nOna?: number;
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
    currentRecordType: 'kvd' | 'tnp' | 'ts' | 'ona' | null;
    currentRecordIndex: number;
    processedRecords: number;
    totalRecords: number;
    createdIntervals?: CreatedProgramData[];
}

export interface ForecastDto {
    id: number;
    dn: string;
    dk: string;
    n_ka: number;
    n_init: number;
}

export interface ShadowDto {
    id: number;
    n_rec: number;
    d_t_in: string;
    d_t_out: string;
    duration: number;
}

export interface ZasvetkaDto {
    id: number;
    n_rec: number;
    d_t_in: string;
    d_t_out: string;
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
    total_intervals: number; // shadows.length + zasvetki.length
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

export interface Kr01ImpulseDto {
    id: number;
    id_main: number;
    n_vit: number;
    date_im: string;
    dlit: number;
    pr_or: number;
    ugl_v: number;
    massa: number;
    n_du: number;
    pr_var: number;
}

export interface Kr01MainDto {
    id: number;
    rnf: number;
    n_ka: number;
    dsf: string;
    n_bc: number;
    n_zad: number;
    k_imp: number;
    dt_zap: string;
    n_form_id: number;
    used: number;
}

export interface Kr01DataResponse {
    main: Kr01MainDto;
    impulses: Kr01ImpulseDto[];
    total_impulses: number;
}

export interface Ro02Dto {
    id: number;
    rnf: number; 
    n_ka: number;    
    dsf: string;   
    data_n: string;  
    data_razv: string; 
    data_k: string | null;
    n_form_id: number; 
}

export interface Ro02DataResponse {
    rotations: Ro02Dto[];
    total_rotations: number;
}

export interface VkiInterval {
    id: string;
    type: 'vki';
    startTime: string;
    endTime: string;
    duration: number;
    title: string;
    color: string;
    opacity: number;
    zIndex: number;
    impulseNumber: number;
    mass: number;
    angle: number;
    nVit: number;
    nDu: number;
    vkiType: 'vki1' | 'vki2';
}

export interface RotationInterval {
    id: string;
    type: 'rotation';
    startTime: string;
    endTime: string;
    duration: number;
    title: string;
    color: string;
    opacity: number;
    zIndex: number;
}

export interface Id02Dto {
    id: number;
    rnf: number;
    n_ka: number;
    d_np: number;
    n_sp: number;
    dsf: string;
    data_zap: string;

    i_msu1: number;      
    i_vd_1: number;      
    i_ik_1: number;      
    vd1_1: number;       
    vd2_1: number;      
    vd3_1: number;      
    ik4_1: number;      
    ik5_1: number;       
    ik6_1: number;      
    ik7_1: number;       
    ik8_1: number;     
    ik9_1: number;      
    ik10_1: number;     
    
    i_msu2: number;      
    i_vd_2: number;      
    i_ik_2: number;      
    vd1_2: number;      
    vd2_2: number;       
    vd3_2: number;      
    ik4_2: number;       
    ik5_2: number;      
    ik6_2: number;       
    ik7_2: number;       
    ik8_2: number;       
    ik9_2: number;      
    ik10_2: number;     
    
    pr_bssd: number;     
    bssd1: number;      
    bssd2: number;    
    pr_zg: number;      
    pr_otkl_zg: number; 
}
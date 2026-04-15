import type { RecordType } from "$lib/constants/recordTypes";
import type { MsuConfig, Ppi } from "./constants";
import type { ConstraintViolation } from "./constraints";
import type { CreatedProgramData } from "./program";

export interface TimeInterval {
    id: string;
    mode: number;
    date: string;
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

    omiData?: {
        id: number;
        idMain: number;
        numOmi: number;
        typeOmi: number;
        dateNach: string;
        dateCon: string;
        dlit: number;
    };

    msu1Config?: MsuConfig;
    msu2Config?: MsuConfig;
    
    hasConflict?: boolean;
    conflictWith?: number[];
    willBeSaved?: boolean;
    nearZasvetka?: boolean;
    inShadow?: boolean;
    willBeSavedInShadow?: boolean;
    shadowPriority?: number;
    zasvetkaConflict?: boolean;
    zasvetkaDistance?: number;
    isAstrocorrection?: boolean;
    constraintViolations?: ConstraintViolation[];
}

export interface ModeCreationForm {
    modeType: number | null;
    ppiNum: number;
    duration: number;
    customerCode: number;
    startTime: string;
    endTime?: string;
    kvdConfig: {
        prMsu: number;
        prBssd: number;
        prZg: number;
    };
    msu1Config: MsuConfig;
    msu2Config: MsuConfig;
    nOna?: number;
    tip?: number;
    reg?: number;
    prBssd?: number;   
    prZg?: number;        
    prOtklZg?: number;
    typeOmi?: number;
}

export interface PpiSelectionModal {
    isOpen: boolean;
    currentRecord: any; // TODO: уточнить тип
    recordType: RecordType | null;
    recordIndex: number;
    totalRecords: number;
    selectedPpi: Ppi | null;
    recordTitle: string;
}

export interface ScheduleCreationState {
    step: 'form' | 'ppi_selection' | 'review' | 'saving';
    currentRecordType: RecordType | null;
    currentRecordIndex: number;
    processedRecords: number;
    totalRecords: number;
    createdIntervals?: CreatedProgramData[];
}
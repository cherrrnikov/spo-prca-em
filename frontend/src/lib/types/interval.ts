import type { Ppi, TsMsuConfig } from "./constants";
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
    
    msu1Config?: TsMsuConfig;
    msu2Config?: TsMsuConfig;
    
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
    kvdConfig: {
        prMsu: number;
        prBssd: number;
        prZg: number;
    };
    msu1Config: TsMsuConfig;
    msu2Config: TsMsuConfig;
    nOna?: number;
    tip?: number;
    reg?: number;
}

export interface PpiSelectionModal {
    isOpen: boolean;
    currentRecord: any; // TODO: уточнить тип
    recordType: 'kvd' | 'tnp' | 'ts' | 'ona' | null;
    recordIndex: number;
    totalRecords: number;
    selectedPpi: Ppi | null;
    recordTitle: string;
}

export interface ScheduleCreationState {
    step: 'form' | 'ppi_selection' | 'review' | 'saving';
    currentRecordType: 'kvd' | 'tnp' | 'ts' | 'ona' | null;
    currentRecordIndex: number;
    processedRecords: number;
    totalRecords: number;
    createdIntervals?: CreatedProgramData[];
}
import type { ShadowInterval, ZasvetkaInterval } from "./forecast";
import type { Id02Dto } from "./id02";
import type { OperatorData } from "./id06";
import type { TimeInterval } from "./interval";
import type { VkiInterval } from "./kr01";
import type { CreatedProgramData, PpiAssignment } from "./program";
import type { RotationInterval } from "./ro02";

export interface ProgramsListItem {
    id: string;
    name: string; // для отображения: ПРЦА 20.01.2026
    date: string;
    intervals: TimeInterval[];
    operatorData: OperatorData | null;
    bortData: Id02Dto | null;
    ppiAssignments: PpiAssignment[];
    createdPrograms: CreatedProgramData[];
    shadowIntervals: ShadowInterval[];
    zasvetkaIntervals: ZasvetkaInterval[];
    vkiIntervals: VkiInterval[];
    rotationIntervals: RotationInterval[];
    numKa: number;
}

export interface AnalysisModalState {
    isOpen: boolean;
    startDate: string;
    endDate: string;
    isLoading: boolean;
}
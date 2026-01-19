import type { OperatorData, PpiAssignment } from "$lib/types/schedule";

export type ScheduleStatus = 'main' | 'corrective';
export type ShootingMode = 'default' | 'no_shooting';
export type MsuGsType = 'msu_gs_1' | 'msu_gs_2';

export interface CreationFormData {
    scheduleStatus: ScheduleStatus;
    selectedDate: string;
    selectedTime: string;
    shootingMode: ShootingMode;
    msuGsType: MsuGsType;
}

export interface CreationFormProps {
    formData: CreationFormData;
    isLoading: boolean;
    onSubmit: () => Promise<void>;
    onCancel: () => void;
}

export interface PpiSelectionFlowProps {
    operatorData: OperatorData;
    assignments: PpiAssignment[];
    onComplete: () => void;
    onCancel: () => void;
}


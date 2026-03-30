import type { OperatorData, PpiAssignment } from "$lib/types";

export type ScheduleStatus = 'main' | 'corrective';

export interface CreationFormData {
    scheduleStatus: ScheduleStatus;
    selectedDate: string;
    selectedTime: string;
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


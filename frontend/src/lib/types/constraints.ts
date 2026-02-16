export type ConstraintModeType = 
    | 'shooting'  // съемка 
    | 'omi'       // ОМИ
    | 'tnp'       // ТНП
    | 'kvd'       // КВД
    | 'ts'        // ТС
    | 'ona'       // Юстировка ОНА
    | 'vki1'      // ВКИ1
    | 'vki2'      // ВКИ2
    | 'rotation'  // разворот
    | 'astrocorrection'; // астрокоррекция

export const MODE_TO_CONSTRAINT_TYPE: Record<number, ConstraintModeType> = {
    1: 'shooting',
    2: 'omi',
    4: 'tnp',
    7: 'kvd',
    8: 'ts',
    6: 'ona',
    9: 'astrocorrection'
};

// Ограничение между режимами
export interface TimeConstraint {
    id: number;
    fromModes: ConstraintModeType[];  
    toModes: ConstraintModeType[]; 
    minGapSeconds: number;
    description: string;
}

// Результат проверки для интервала
export interface ConstraintViolation {
    constraintId: number;
    withIntervalId: string;
    requiredGap: number;
    actualGap: number;
    direction: 'before' | 'after' | 'overlap';
}
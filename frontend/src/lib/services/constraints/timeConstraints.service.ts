import type { ConstraintModeType, TimeConstraint } from '$lib/types/constraints';

export class TimeConstraintsService {
    private static readonly CONSTRAINTS: TimeConstraint[] = [
        {
            id: 1,
            fromModes: ['shooting', 'omi', 'tnp', 'kvd', 'ts', 'ona'],
            toModes: ['vki1'],
            minGapSeconds: 4820,
            description: 'Между Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА и ВКИ1 должно быть не менее 4820 сек'
        }
    ];

    static getAllConstraints(): TimeConstraint[] {
        return this.CONSTRAINTS;
    }

    static getConstraintsForMode(mode: ConstraintModeType): {
        asFrom: TimeConstraint[]; 
        asTo: TimeConstraint[];  
    } {
        return {
            asFrom: this.CONSTRAINTS.filter(c => c.fromModes.includes(mode)),
            asTo: this.CONSTRAINTS.filter(c => c.toModes.includes(mode))
        };
    }
}
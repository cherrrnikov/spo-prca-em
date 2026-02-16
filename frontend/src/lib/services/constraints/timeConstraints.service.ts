import type { ConstraintModeType, TimeConstraint } from '$lib/types/constraints';

export class TimeConstraintsService {
    private static readonly CONSTRAINTS: TimeConstraint[] = [
        {
            id: 1,
            fromModes: ['shooting', 'omi', 'tnp', 'kvd', 'ts', 'ona'],
            toModes: ['vki1'],
            minGapSeconds: 4820,
            description: 'Между Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА и ВКИ1 должно быть не менее 4820 сек'
        },
        {
            id: 2,
            fromModes: ['shooting', 'omi', 'tnp', 'kvd', 'ts', 'ona'],
            toModes: ['vki2'],
            minGapSeconds: 1810,
            description: 'Между Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА и ВКИ2 должно быть не менее 1810 сек'
        },
        {
            id: 3,
            fromModes: ['shooting', 'omi', 'tnp', 'kvd', 'ts', 'ona'],
            toModes: ['rotation'],
            minGapSeconds: 3900,
            description: 'Между Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА и сезонным разворотом должно быть не менее 3900 сек'
        },
        {
            id: 6,
            fromModes: ['astrocorrection'],
            toModes: ['shooting', 'tnp', 'kvd', 'ts', 'ona'],
            minGapSeconds: 300,
            description: 'Между астрокоррекцией и Шт./Уч. съемки,ТНП,КВД,ТС,Юст.ОНА должно быть не менее 300 сек'
        },
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
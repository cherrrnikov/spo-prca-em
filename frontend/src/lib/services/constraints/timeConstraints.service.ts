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
        {
            id: 7,
            fromModes: ['shooting', 'tnp', 'kvd', 'ts', 'ona'],
            toModes: ['astrocorrection'],
            minGapSeconds: -10,
            description: 'Между Шт./Уч. съемки,ТНП,КВД,ТС,Юст.ОНА и астрокоррекцией разрешено перекрытие 10 сек'
        },
        {
            id: 10,
            fromModes: ['shooting','ts'],
            toModes: ['omi'],
            minGapSeconds: 60,
            markTarget: 'from',
            description: 'Между Шт./Уч. съемки, ТС и ОМИ должно быть не менее 60 сек'
        },
        {
            id: 11,
            fromModes: ['omi'],
            toModes: ['shooting','ts'], 
            minGapSeconds: 60,
            markTarget: 'to',
            description: 'Между ОМИ и Шт./Уч. съемки, ТС должно быть не менее 60 сек'
        },
        {
            id: 14,
            fromModes: ['shadow'],
            toModes: ['shooting', 'ts'], 
            minGapSeconds: 300,
            markTarget: 'to',
            checkFromStart: true, // проверяем от начала тени
            description: 'Между началом тени и началом шт. съемки, ТС должно быть не менее 300 сек'
        },
        {
            id: 15,
            fromModes: ['shooting', 'ts'],
            toModes: ['shadow'],
            minGapSeconds: 300,
            checkToEnd: true, // проверяем от конца from до конца to
            markTarget: 'from', // помечаем shooting/ts
            description: 'Между концом съемки, ТС и концом тени должно быть не менее 300 сек'
        },
        {
            id: 16,
            fromModes: ['vki1'],
            toModes: ['shooting', 'omi', 'tnp', 'kvd', 'ts', 'ona'],
            minGapSeconds: 6250,
            description: 'Между ВКИ1 и Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА должно быть не менее 6250 сек'
        },
        {
            id: 17,
            fromModes: ['vki2'],
            toModes: ['shooting', 'omi', 'tnp', 'kvd', 'ts', 'ona'],
            minGapSeconds: 1210,
            description: 'Между ВКИ2 и Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА должно быть не менее 1210 сек'
        },
        {
            id: 38,
            fromModes: ['shooting', 'omi', 'tnp', 'kvd', 'ts', 'ona'],
            toModes: ['zasvetka'],
            minGapSeconds: 60,
            description: 'Между Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА и засветками должно быть не менее 60 сек'
        },
        {
            id: 48,
            fromModes: ['rotation'],
            toModes: ['shooting', 'omi', 'tnp', 'kvd', 'ts', 'ona'],
            minGapSeconds: 5700,
            description: 'Между Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА и сезонным разворотом должно быть не менее 5700 сек'
        },
        {
            id: 81,
            fromModes: ['zasvetka'],
            toModes: ['shooting', 'tnp', 'kvd', 'ts', 'ona'],
            minGapSeconds: 300,
            description: 'Между засветками и Шт./Уч. съемки,ТНП,КВД,ТС,Юст.ОНА должно быть не менее 300 сек'
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
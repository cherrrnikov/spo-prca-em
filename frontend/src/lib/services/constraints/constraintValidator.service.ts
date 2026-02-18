import { MODE_TO_CONSTRAINT_TYPE, type ConstraintViolation, type TimeConstraint } from '$lib/types/constraints';
import type { RotationInterval, ShadowInterval, TimeInterval, VkiInterval, ZasvetkaInterval } from '$lib/types/schedule';
import { TimeUtils } from '$lib/utils/time';
import { TimeConstraintsService } from './timeConstraints.service';

type CheckableInterval = {
    id: string;
    startSeconds: number;
    endSeconds: number;
    modeType: string;
    isAstro: boolean;
    originalStart: string;
    originalEnd: string;
};

export class ConstraintValidator {
    static validate(
        regularIntervals: TimeInterval[],
        vkiIntervals: VkiInterval[],
        rotationIntervals: RotationInterval[],
        astroIntervals: TimeInterval[],
        shadowIntervals: ShadowInterval[],
        zasvetkaIntervals: ZasvetkaInterval[]
    ): Map<string, ConstraintViolation[]> {
        const violations = new Map<string, ConstraintViolation[]>();

        // 1. Группируем интервалы по типам
        const intervalsByType = new Map<string, CheckableInterval[]>();
        
        astroIntervals.forEach(a => {
            const type = 'astrocorrection';  
            if (!intervalsByType.has(type)) {
                intervalsByType.set(type, []);
            }
            intervalsByType.get(type)!.push({
                id: a.id,
                startSeconds: TimeUtils.timeToSeconds(a.startTime),
                endSeconds: TimeUtils.timeToSeconds(a.endTime),
                modeType: type,
                isAstro: true,
                originalStart: a.startTime,
                originalEnd: a.endTime
            });
        });

        regularIntervals.forEach(i => {
            const type = MODE_TO_CONSTRAINT_TYPE[i.mode] || 'unknown';
            if (!intervalsByType.has(type)) {
                intervalsByType.set(type, []);
            }
            intervalsByType.get(type)!.push({
                id: i.id,
                startSeconds: TimeUtils.timeToSeconds(i.startTime),
                endSeconds: TimeUtils.timeToSeconds(i.endTime),
                modeType: type,
                isAstro: i.isAstrocorrection || false,
                originalStart: i.startTime,
                originalEnd: i.endTime
            });
        });

        vkiIntervals.forEach(v => {
            const type = v.vkiType;
            if (!intervalsByType.has(type)) {
                intervalsByType.set(type, []);
            }
            intervalsByType.get(type)!.push({
                id: v.id,
                startSeconds: TimeUtils.timeToSeconds(v.startTime),
                endSeconds: TimeUtils.timeToSeconds(v.endTime),
                modeType: type,
                isAstro: true,
                originalStart: v.startTime,
                originalEnd: v.endTime
            });
        });

        rotationIntervals.forEach(r => {
            const type = 'rotation';
            if (!intervalsByType.has(type)) {
                intervalsByType.set(type, []);
            }
            intervalsByType.get(type)!.push({
                id: r.id,
                startSeconds: TimeUtils.timeToSeconds(r.startTime),
                endSeconds: TimeUtils.timeToSeconds(r.endTime),
                modeType: type,
                isAstro: true,
                originalStart: r.startTime,
                originalEnd: r.endTime
            });
        });

        shadowIntervals.forEach(s => {
                const type = 'shadow';
                if (!intervalsByType.has(type)) {
                    intervalsByType.set(type, []);
                }
                intervalsByType.get(type)!.push({
                    id: s.id,
                    startSeconds: TimeUtils.timeToSeconds(s.startTime),
                    endSeconds: TimeUtils.timeToSeconds(s.endTime),
                    modeType: type,
                    isAstro: false,
                    originalStart: s.startTime,
                    originalEnd: s.endTime
                });
            });

        zasvetkaIntervals.forEach(z => {
            const type = 'zasvetka';
            if (!intervalsByType.has(type)) {
                intervalsByType.set(type, []);
            }
            intervalsByType.get(type)!.push({
                id: z.id,
                startSeconds: TimeUtils.timeToSeconds(z.startTime),
                endSeconds: TimeUtils.timeToSeconds(z.endTime),
                modeType: type,
                isAstro: false,
                originalStart: z.startTime,
                originalEnd: z.endTime
            });
        });

        // 2. Получаем ограничения
        const constraints = TimeConstraintsService.getAllConstraints();

        // 3. Для каждого ограничения проверяем только нужные пары типов
        for (const constraint of constraints) {
            const fromIntervals: CheckableInterval[] = [];
            for (const fromType of constraint.fromModes) {
                const intervals = intervalsByType.get(fromType);
                if (intervals) {
                    fromIntervals.push(...intervals);
                }
            }

            const toIntervals: CheckableInterval[] = [];
            for (const toType of constraint.toModes) {
                const intervals = intervalsByType.get(toType);
                if (intervals) {
                    toIntervals.push(...intervals);
                }
            }

            // Сортируем интервалы по времени начала для эффективного поиска
            fromIntervals.sort((a, b) => a.startSeconds - b.startSeconds);
            toIntervals.sort((a, b) => a.startSeconds - b.startSeconds);

            // 4. Проверяем каждую пару from -> to
            for (const from of fromIntervals) {
                if (constraint.minGapSeconds < 0) {
                    // Отрицательный зазор - разрешено небольшое перекрытие
                    this.checkNegativeGapConstraint(violations, from, toIntervals, constraint);
                } else {
                    // Положительный зазор - стандартная проверка
                    this.checkPositiveGapConstraint(violations, from, toIntervals, constraint);
                }
            }
        }

        return violations;
    }

    private static checkPositiveGapConstraint(
        violations: Map<string, ConstraintViolation[]>,
        from: CheckableInterval,
        toIntervals: CheckableInterval[],
        constraint: TimeConstraint
    ) {
        if (constraint.checkFromStart) {
            // Специальная логика для теней - проверяем от НАЧАЛА from
            this.checkFromStartConstraint(violations, from, toIntervals, constraint);
            return;
        }

        if (constraint.checkToEnd) {
            this.checkToEndConstraint(violations, from, toIntervals, constraint);
            return;
        }

        // Стандартная логика для остальных (от конца from)
        let left = 0;
        let right = toIntervals.length - 1;
        let firstIndex = toIntervals.length;

        while (left <= right) {
            const mid = Math.floor((left + right) / 2);
            if (toIntervals[mid].startSeconds >= from.endSeconds) {
                firstIndex = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        // 1. Проверяем пересечения
        for (let k = 0; k < firstIndex; k++) {
            const to = toIntervals[k];
            if (to.endSeconds > from.startSeconds) {
                this.addViolation(violations, from, to, constraint, 0, 'overlap');
                this.addViolation(violations, to, from, constraint, 0, 'overlap');
            }
        }

        // 2. Проверяем зазоры
        for (let k = firstIndex; k < toIntervals.length; k++) {
            const to = toIntervals[k];
            const gap = to.startSeconds - from.endSeconds;
            
            if (gap < constraint.minGapSeconds) {
                const markTarget = constraint.markTarget || 'both';
                
                if (markTarget === 'from' || markTarget === 'both') {
                    this.addViolation(violations, from, to, constraint, gap, 'after');
                }
                if (markTarget === 'to' || markTarget === 'both') {
                    this.addViolation(violations, to, from, constraint, gap, 'before');
                }
            } else {
                break;
            }
        }
    }

    private static checkFromStartConstraint(
        violations: Map<string, ConstraintViolation[]>,
        from: CheckableInterval, // тень
        toIntervals: CheckableInterval[], // shooting/ts
        constraint: TimeConstraint
    ) {
        // Сортируем to интервалы по времени начала
        toIntervals.sort((a, b) => a.startSeconds - b.startSeconds);
        
        for (const to of toIntervals) {
            // Проверяем только to после начала тени
            if (to.startSeconds >= from.startSeconds) {
                const gap = to.startSeconds - from.startSeconds; // от НАЧАЛА тени до НАЧАЛА to
                if (gap < constraint.minGapSeconds) {
                    // Помечаем только to (shooting/ts) согласно markTarget: 'to'
                    this.addViolation(violations, to, from, constraint, gap, 'after');
                }
            }
        }
    }

    private static checkToEndConstraint(
        violations: Map<string, ConstraintViolation[]>,
        from: CheckableInterval, // shooting/ts
        toIntervals: CheckableInterval[], // тени
        constraint: TimeConstraint
    ) {
        // Сортируем тени по времени конца
        toIntervals.sort((a, b) => a.endSeconds - b.endSeconds);
        
        for (const to of toIntervals) {
            // Проверяем только тени, которые заканчиваются после конца from
            if (to.endSeconds >= from.endSeconds) {
                const gap = to.endSeconds - from.endSeconds; // от КОНЦА from до КОНЦА to
                if (gap < constraint.minGapSeconds) {
                    // Помечаем только from (shooting/ts) согласно markTarget: 'from'
                    this.addViolation(violations, from, to, constraint, gap, 'after');
                }
            }
        }
    }

    private static checkNegativeGapConstraint(
        violations: Map<string, ConstraintViolation[]>,
        from: CheckableInterval,  
        toIntervals: CheckableInterval[],  
        constraint: TimeConstraint
    ) {
        const allowedOverlap = -constraint.minGapSeconds; // 10 секунд
        
        for (const to of toIntervals) {
            // Проверяем только случай, когда from (shooting) заканчивается после начала to (astrocorrection)
            // и начинается до или во время to
            const isFromBeforeTo = from.startSeconds <= to.startSeconds;
            const hasOverlap = from.endSeconds > to.startSeconds;
            
            if (isFromBeforeTo && hasOverlap) {
                // Вычисляем глубину перекрытия (сколько секунд from заехал на to)
                const overlapSeconds = from.endSeconds - to.startSeconds;
                
                // Если перекрытие больше разрешенного
                if (overlapSeconds > allowedOverlap) {
                    this.addViolation(violations, from, to, constraint, -overlapSeconds, 'overlap');
                    this.addViolation(violations, to, from, constraint, -overlapSeconds, 'overlap');
                } 
            }
            
            // Если from начинается после to - это уже случай для id 6 (астрокоррекция слева)
            // Если нет перекрытия - зазор может быть любым, ограничений нет
        }
    }

    private static addViolation(
        violations: Map<string, ConstraintViolation[]>,
        target: CheckableInterval,
        other: CheckableInterval,
        constraint: TimeConstraint,
        gap: number,
        direction: 'before' | 'after' | 'overlap'
    ) {
        const violation: ConstraintViolation = {
            constraintId: constraint.id,
            withIntervalId: other.id,
            requiredGap: constraint.minGapSeconds,
            actualGap: gap,
            direction: direction,
        };

        if (!violations.has(target.id)) {
            violations.set(target.id, []);
        }
        violations.get(target.id)!.push(violation);
    }
}
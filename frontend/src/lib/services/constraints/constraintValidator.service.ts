import { MODE_TO_CONSTRAINT_TYPE, type ConstraintViolation, type TimeConstraint } from '$lib/types/constraints';
import type { RotationInterval, TimeInterval, VkiInterval } from '$lib/types/schedule';
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
        astroIntervals: TimeInterval[]
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
                // Ищем все to-интервалы, которые начинаются после from
                // Бинарный поиск первого to с startSeconds >= from.endSeconds
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

                // Проверяем пересечения (to начался до окончания from)
                for (let k = 0; k < firstIndex; k++) {
                    const to = toIntervals[k];
                    if (to.endSeconds > from.startSeconds) { // Есть пересечение
                        this.addViolation(violations, from, to, constraint, 0, 'overlap');
                        this.addViolation(violations, to, from, constraint, 0, 'overlap');
                    }
                }

                // Проверяем зазоры для to после from
                for (let k = firstIndex; k < toIntervals.length; k++) {
                    const to = toIntervals[k];
                    const gap = to.startSeconds - from.endSeconds;
                    
                    if (gap < constraint.minGapSeconds) {
                        this.addViolation(violations, from, to, constraint, gap, 'after');
                        this.addViolation(violations, to, from, constraint, gap, 'before');
                    } else {
                        // Так как массив отсортирован, все следующие to будут с еще большим gap
                        break;
                    }
                }
            }
        }

        return violations;
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
            direction: direction
        };

        if (!violations.has(target.id)) {
            violations.set(target.id, []);
        }
        violations.get(target.id)!.push(violation);
    }
}
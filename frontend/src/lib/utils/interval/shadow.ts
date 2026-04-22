import type { ShadowInterval, TimeInterval } from '$lib/types';
import { TimeUtils } from '../time';

export function checkShadowPriority(
    intervals: TimeInterval[],
    shadowIntervals: ShadowInterval[] = []
): TimeInterval[] {
    if (!shadowIntervals || shadowIntervals.length === 0) {
        return intervals.map(interval => ({
            ...interval,
            inShadow: false,
            shadowPriority: 0,
            willBeSavedInShadow: false
        }));
    }

    const updatedIntervals = intervals.map(interval => ({
        ...interval,
        inShadow: false,
        shadowPriority: 0,
        willBeSavedInShadow: false
    }));

    for (const shadow of shadowIntervals) {
        const shadowStart = TimeUtils.timeToSeconds(shadow.startTime);
        const shadowEnd = TimeUtils.timeToSeconds(shadow.endTime);
        const shadowCenter = shadowStart + (shadowEnd - shadowStart) / 2;

        const intervalsInThisShadow = updatedIntervals.filter(interval => {
            const intervalStart = TimeUtils.timeToSeconds(interval.startTime);
            const intervalEnd = TimeUtils.timeToSeconds(interval.endTime);
            return intervalStart >= shadowStart && intervalEnd <= shadowEnd;
        });

        if (intervalsInThisShadow.length === 0) continue;

        intervalsInThisShadow.forEach(interval => {
            interval.inShadow = true;
            const intervalCenter = TimeUtils.timeToSeconds(interval.startTime) + 
                                 (TimeUtils.timeToSeconds(interval.endTime) - TimeUtils.timeToSeconds(interval.startTime)) / 2;
            interval.shadowPriority = Math.abs(intervalCenter - shadowCenter);
        });

        const sortedIntervals = [...intervalsInThisShadow].sort((a, b) => {
            if (a.shadowPriority !== b.shadowPriority) {
                return a.shadowPriority - b.shadowPriority;
            }
            return TimeUtils.timeToSeconds(a.startTime) - TimeUtils.timeToSeconds(b.startTime);
        });

        const winner = sortedIntervals[0];
        
        intervalsInThisShadow.forEach(interval => {
            interval.willBeSavedInShadow = false;
        });
        
        winner.willBeSavedInShadow = true;

        if (winner.mode === 8 || winner.mode === 1) {
            // Глубокая копия — не мутируем оригинальные объекты в сторе
            if (winner.msu1Config) {
                winner.msu1Config = {
                    ...winner.msu1Config,
                    prVdMsu: 0,
                    vd1: 0,
                    vd2: 0,
                    vd3: 0
                };
                // Если нет ни одного активного канала — МСУ не задействован
                const hasAnyMsu1Channel = winner.msu1Config.ik4 || winner.msu1Config.ik5 ||
                    winner.msu1Config.ik6 || winner.msu1Config.ik7 || winner.msu1Config.ik8 ||
                    winner.msu1Config.ik9 || winner.msu1Config.ik10;
                if (!hasAnyMsu1Channel) {
                    winner.msu1Config = { ...winner.msu1Config, prMsu: 0 };
                }
            }

            if (winner.msu2Config) {
                winner.msu2Config = {
                    ...winner.msu2Config,
                    prVdMsu: 0,
                    vd1: 0,
                    vd2: 0,
                    vd3: 0
                };
                const hasAnyMsu2Channel = winner.msu2Config.ik4 || winner.msu2Config.ik5 ||
                    winner.msu2Config.ik6 || winner.msu2Config.ik7 || winner.msu2Config.ik8 ||
                    winner.msu2Config.ik9 || winner.msu2Config.ik10;
                if (!hasAnyMsu2Channel) {
                    winner.msu2Config = { ...winner.msu2Config, prMsu: 0 };
                }
            }

            if (winner.msuData) {
                winner.msuData = {
                    ...winner.msuData,
                    prMsu1: winner.msu1Config?.prMsu ?? 0,  // берём из уже пересчитанного конфига
                    vd1Msu1: 0, vd2Msu1: 0, vd3Msu1: 0,
                    prMsu2: winner.msu2Config?.prMsu ?? 0,
                    vd2Msu2: 0, vd1Msu2: 0, vd3Msu2: 0
                };
            }

            // Если после обнуления ВД все каналы пустые — интервал не имеет смысла
            const msu1HasChannels = winner.msu1Config && (
                winner.msu1Config.ik4 || winner.msu1Config.ik5 || winner.msu1Config.ik6 ||
                winner.msu1Config.ik7 || winner.msu1Config.ik8 || winner.msu1Config.ik9 ||
                winner.msu1Config.ik10
            );
            const msu2HasChannels = winner.msu2Config && (
                winner.msu2Config.ik4 || winner.msu2Config.ik5 || winner.msu2Config.ik6 ||
                winner.msu2Config.ik7 || winner.msu2Config.ik8 || winner.msu2Config.ik9 ||
                winner.msu2Config.ik10
            );
            winner.emptyMsu = !msu1HasChannels && !msu2HasChannels;
            if (winner.emptyMsu) {
                winner.willBeSaved = false;
            }
        }

        intervalsInThisShadow
            .filter(i => i !== winner)
            .forEach(interval => {
                interval.willBeSaved = false;
            });
    }

    return updatedIntervals;
}
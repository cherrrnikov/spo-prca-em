import { ONA_CONSTRAINT_IDS } from '$lib/config/schedule.config';
import type { TimeInterval } from '$lib/types';
import { TooltipFormatter } from '$lib/utils/tooltipFormatter';
import { get } from 'svelte/store';
import type { createStores } from '../stores';

export function createUiHelpers(stores: ReturnType<typeof createStores>) {
    const { createdPrograms } = stores;

    function getIntervalColor(interval: TimeInterval): string {
        if (interval.emptyMsu) {
            return '#ffffff';
        }

        if (interval.inShadow && interval.willBeSavedInShadow) {
            return '#ff69b4';
        }

        if (interval.hasConflict) {
            return '#ff0000';
        }
        
        if (interval.constraintViolations?.length) {
            const onlySpecialViolations = interval.constraintViolations.every(
                v => ONA_CONSTRAINT_IDS.includes(v.constraintId)
            );
            return onlySpecialViolations ? '#ff0000' : '#ffffff';
        }

        if (interval.zasvetkaConflict || interval.nearZasvetka) {
            return '#ffffff';
        }
        
        if (interval.isAstrocorrection) {
            return '#1e40af';
        }
        
        return interval.color;
    }

    function getIntervalTooltip(interval: TimeInterval): string {
        const programData = get(createdPrograms).find(p => p.timeInterval.id === interval.id);
        
        if (programData) {
            return TooltipFormatter.formatTooltip(interval, programData.modeData);
        }

        if (interval.isAstrocorrection) {
            return `Астрокоррекция ${interval.startTime} - ${interval.endTime}`;
        }
        
        return interval.title || '';
    }

    return {
        getIntervalColor,
        getIntervalTooltip
    };
}
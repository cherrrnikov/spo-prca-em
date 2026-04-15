import type { ProgramsListItem } from '$lib/types/analysis';
import { getDefaultIntervalFlags } from '$lib/utils/interval';
import { TooltipFormatter } from '$lib/utils/tooltipFormatter';
import { get } from 'svelte/store';
import { CityService } from '../../../../features/services/utils/cities.service';
import type { createStores } from '../stores';

export function createCleanup(stores: ReturnType<typeof createStores>) {
    const {
        intervals,
        createdPrograms,
        isEditing,
        selectedMode,
        editingInterval,
        selectedIntervalId,
        creationMode,
        operatorDataLoaded,
        isReadOnly,
        programsList  // ← добавить
    } = stores;

    function cleanupSingleProgram(program: ProgramsListItem): ProgramsListItem {
        // 1. Фильтруем интервалы: оставляем только willBeSaved = true и астрокоррекции
        const savedIntervals = program.intervals.filter(i => 
            i.willBeSaved === true || i.isAstrocorrection === true
        );
        
        // 2. Создаем карту tooltip из createdPrograms
        const tooltipMap = new Map<string, string>();
        program.createdPrograms.forEach(prog => {
            const interval = prog.timeInterval;
            if (interval.willBeSaved === true && !interval.isAstrocorrection) {
                const tooltip = TooltipFormatter.formatTooltip(interval, prog.modeData);
                tooltipMap.set(interval.id, tooltip);
            }
        });
        
        // 3. Очищаем интервалы
        const restoredIntervals = savedIntervals.map(interval => {
            let newInterval = { ...interval };
            
            // Сбрасываем все временные флаги
            newInterval = {
                ...newInterval,
                constraintViolations: [],
                ...getDefaultIntervalFlags(),
                inShadow: false,
                willBeSavedInShadow: false,
                shadowPriority: 0
            };
            
            // Восстанавливаем цвет по ППИ
            if (!interval.isAstrocorrection) {
                const originalColor = CityService.getColorByPpi(interval.ppi || 1);
                newInterval.color = originalColor;
            }
            
            // Добавляем сохраненный tooltip
            const savedTooltip = tooltipMap.get(interval.id);
            if (savedTooltip) {
                newInterval.title = savedTooltip;
            }
            
            return newInterval;
        });
        
        return {
            ...program,
            intervals: restoredIntervals,
            createdPrograms: []  // очищаем createdPrograms
        };
    }

    function cleanupAfterSave() {
        console.log("🔧 cleanupAfterSave ВЫЗВАН!");
        console.log("=== ОЧИСТКА СЕТКИ ПОСЛЕ СОХРАНЕНИЯ ===");
        
        // 1. Очищаем текущую активную ПРЦА (стора)
        const currentIntervals = get(intervals);
        const currentCreatedPrograms = get(createdPrograms);
        
        const tooltipMap = new Map<string, string>();
        currentCreatedPrograms.forEach(program => {
            const interval = program.timeInterval;
            if (interval.willBeSaved === true && !interval.isAstrocorrection) {
                const tooltip = TooltipFormatter.formatTooltip(interval, program.modeData);
                tooltipMap.set(interval.id, tooltip);
            }
        });
        
        const savedIntervals = currentIntervals.filter(i => 
            i.willBeSaved === true || i.isAstrocorrection === true
        );
        
        const restoredIntervals = savedIntervals.map(interval => {
            let newInterval = { ...interval };
            newInterval = {
                ...newInterval,
                constraintViolations: [],
                ...getDefaultIntervalFlags(),
                inShadow: false,
                willBeSavedInShadow: false,
                shadowPriority: 0
            };
            
            if (!interval.isAstrocorrection) {
                const originalColor = CityService.getColorByPpi(interval.ppi || 1);
                newInterval.color = originalColor;
            }
            
            const savedTooltip = tooltipMap.get(interval.id);
            if (savedTooltip) {
                newInterval.title = savedTooltip;
            }
            
            return newInterval;
        });
        
        intervals.set(restoredIntervals);
        createdPrograms.set([]);
        
        // 2. Очищаем все ПРЦА в programsList
        const currentProgramsList = get(programsList);
        const cleanedProgramsList = currentProgramsList.map(program => cleanupSingleProgram(program));
        programsList.set(cleanedProgramsList);
        
        // 3. Выключаем режим редактирования
        isEditing.set(false);
        selectedMode.set(null);
        editingInterval.set(null);
        selectedIntervalId.set(null);
        creationMode.set(null);
        operatorDataLoaded.set(false);
        console.log("🔧 Устанавливаем isReadOnly = true");
        isReadOnly.set(true);
        console.log("🔧 isReadOnly после установки:", get(isReadOnly));
        
        console.log(`Очистка завершена. Сохранено интервалов в текущей ПРЦА: ${restoredIntervals.length}`);
        console.log(`Очищено ПРЦА в списке: ${cleanedProgramsList.length}`);
    }

    return {
        cleanupAfterSave
    };
}
<script lang="ts">
    import type { ShadowInterval, TimeInterval, WorkMode, ZasvetkaInterval } from '$lib/types/schedule';
    
    let {
        intervals,
        shadowIntervals = [],
        zasvetkaIntervals = [],
        workModes = [],
        onModeSelect
    } = $props<{
        intervals: TimeInterval[];
        shadowIntervals?: ShadowInterval[];
        zasvetkaIntervals?: ZasvetkaInterval[];
        workModes?: WorkMode[];
        onModeSelect?: (modeId: number) => void;
    }>();

    const HOURS = Array.from({length: 24}, (_, i) => i);
    const MIN_CELL_WIDTH = 40;
    const MAX_CELL_WIDTH = 69;
    const ROW_HEIGHT = 40;
    const TIME_HEIGHT = 40;
    
    let containerWidth = $state(0);
    let cellWidth = $derived(0);
    let gridContainer = $state<HTMLDivElement>();
    
    let selectedMode = $state<number | null>(null);

    function updateContainerWidth() {
        if (gridContainer) {
            containerWidth = gridContainer.offsetWidth;
        }
    }
    
    $effect(() => {
        if (gridContainer) {
            updateContainerWidth();
            
            const resizeObserver = new ResizeObserver(() => {
                updateContainerWidth();
            });
            
            resizeObserver.observe(gridContainer);
            
            return () => {
                resizeObserver.disconnect();
            };
        }
    });
    
    $effect(() => {
        if (containerWidth > 0) {
            const availableWidth = containerWidth;
            const calculatedWidth = availableWidth / 24;
            
            cellWidth = Math.max(MIN_CELL_WIDTH, Math.min(calculatedWidth, MAX_CELL_WIDTH));
        }
    });

    const filteredIntervals = $derived(
        selectedMode 
            ? intervals.filter((interval: { mode: number | null; }) => interval.mode === selectedMode)
            : []
    );

    function timeToMinutes(time: string): number {
        const [hours, minutes] = time.split(':').map(Number);
        return hours * 60 + (minutes || 0);
    }

    function minutesToPixels(minutes: number): number {
        return (minutes / 60) * cellWidth;
    }

    function getPositionForInterval(
        startTime: string, 
        endTime: string, 
        modeIndex: number
    ) {
        const startMinutes = timeToMinutes(startTime);
        const endMinutes = timeToMinutes(endTime);
        const durationMinutes = endMinutes - startMinutes;

        return {
            left: `${minutesToPixels(startMinutes)}px`,
            width: `${minutesToPixels(durationMinutes)}px`,
            top: `${TIME_HEIGHT + modeIndex * ROW_HEIGHT - 15}px`,
            height: `${ROW_HEIGHT - 10}px`
        };
    }

    function getPositionedIntervals() {
        if (!workModes || workModes.length === 0) {
            console.warn('workModes пуст или не определен');
            return [];
        }

        const allPositionedIntervals: any[] = [];
        
        intervals.forEach((interval: { mode: any; id: any; startTime: any; endTime: any; }) => {
            const modeIndex = workModes.findIndex((m: { id: any; }) => m.id === interval.mode);
            
            if (modeIndex === -1) {
                console.warn(`Mode ${interval.mode} not found for interval ${interval.id}`);
                return;
            }
            
            allPositionedIntervals.push({
                ...interval,
                type: 'schedule',
                modeIndex,
                position: getPositionForInterval(interval.startTime, interval.endTime, modeIndex)
            });
        });
        
        if (shadowIntervals.length > 0) {
            shadowIntervals.forEach((shadow: { startTime: string; endTime: string; }) => {
                allPositionedIntervals.push({
                    ...shadow,
                    type: 'shadow',
                    modeIndex: -1, // Специальный индекс для полных полос
                    position: {
                        left: getPositionForInterval(shadow.startTime, shadow.endTime, 0).left,
                        width: getPositionForInterval(shadow.startTime, shadow.endTime, 0).width,
                        top: `0px`, // Начинается сразу после шкалы времени
                        height: `${ROW_HEIGHT * (workModes.length + 1)}px` // На всю высоту всех режимов
                    }
                });
            });
        }
        
        if (zasvetkaIntervals.length > 0) {
            zasvetkaIntervals.forEach((zasvetka: { startTime: string; endTime: string; }) => {
                allPositionedIntervals.push({
                    ...zasvetka,
                    type: 'zasvetka',
                    modeIndex: -1,
                    position: {
                        left: getPositionForInterval(zasvetka.startTime, zasvetka.endTime, 0).left,
                        width: getPositionForInterval(zasvetka.startTime, zasvetka.endTime, 0).width,
                        top: `0px`,
                        height: `${ROW_HEIGHT * (workModes.length + 1)}px`
                    }
                });
            });
        }
        
        return allPositionedIntervals;
    }
    
    function selectMode(modeId: number) {
        selectedMode = selectedMode === modeId ? null : modeId;
        const modeToSend = selectedMode || modeId;
        
        onModeSelect?.(modeToSend);
    }
</script>

<div class="schedule-grid">
    {#if workModes && workModes.length > 0}
        <div class="modes-container">
            {#each workModes as mode, i}
                <div class="mode-label-container" style="top: {1.4 * TIME_HEIGHT + i * ROW_HEIGHT}px">
                    <label class="mode-checkbox">
                        <input 
                            type="radio" 
                            name="workMode"
                            value={mode.id}
                            checked={selectedMode === mode.id}
                            on:change={() => selectMode(mode.id)}
                        />
                        <span class="mode-text">{mode.label}</span>
                    </label>
                </div>
            {/each}
        </div>
    {/if}
    
    <div 
        class="schedule-grid_container"
        bind:this={gridContainer}
    >
        <div class="time-scale top-scale">
            {#each HOURS as hour}
                <div 
                    class="hour-marker" 
                    style="left: {(hour * cellWidth)}px; width: {cellWidth}px"
                >
                    <div class="hour-label">
                        {hour.toString().padStart(2, '0')}:00
                    </div>
                </div>
            {/each}
        </div>
        
        <div class="time-scale bottom-scale">
            {#each HOURS as hour}
                <div 
                    class="hour-marker" 
                    style="left: {(hour * cellWidth)}px; width: {cellWidth}px"
                >
                    <div class="hour-label">
                        {hour.toString().padStart(2, '0')}:00
                    </div>
                </div>
            {/each}
        </div>

        <div 
            class="grid-area"
            style="
                grid-template-columns: repeat(24, {cellWidth}px); 
                width: {cellWidth * 24 + 5}px;
                --cell-width: {cellWidth}px;
            "
        >
            {#each getPositionedIntervals() as item}
                <div 
                    class="interval interval-{item.type}" 
                    style="
                        left: {item.position.left}; 
                        width: {item.position.width}; 
                        top: {item.position.top}; 
                        height: {item.position.height};
                        background: {item.color};
                        opacity: {item.opacity || 1};
                        z-index: {item.zIndex || 10};
                    "
                    title="{(item.title || '') + ' ' + item.startTime + '-' + item.endTime}"
                >
                    {#if item.type === 'schedule'}
                        <div class="interval-content" style="background: {item.color};">
                        </div>
                    {/if}
                </div>
            {/each}
        </div>
    </div>
</div>

<style>
    .schedule-grid {
        display: flex;
        justify-content: space-between;
        position: relative;
        width: 100%;
        min-height: 410px;
        background: white;
        padding-right: 20px;
        overflow-x: auto; 
    }
    
    .time-scale {
        position: absolute;
        height: 40px;
        width: calc(100% - 1px);
        z-index: 20;
        white-space: nowrap;
    }
    
    .top-scale {
        top: 0;
    }
    
    .bottom-scale {
        bottom: 0;
    }
    
    .hour-marker {
        position: absolute;
        text-align: center;
    }
    
    .hour-label {
        position: relative;
        width: 100%;
        text-align: start;
        font-size: clamp(0.7rem, 1vw, 0.85rem); 
        color: #4a5568;
        font-weight: 500;
        pointer-events: none;
    }
    
    .top-scale .hour-label {
        top: 15px;
    }
    
    .bottom-scale .hour-label {
        bottom: 0;
    }
    
    .modes-container {
        position: relative;
        width: 11%;
        min-height: 340px;
    }

    .schedule-grid_container {
        position: relative;
        width: 89%;
        min-height: 340px;
        overflow-x: visible;
    }
    
    .grid-area {
        position: absolute;
        top: 40px;
        left: 0;
        border: 2px solid #4a5568;
        
        display: grid;
        grid-template-rows: repeat(8, 40px);
        
        background-image: 
            repeating-linear-gradient(
                to right,
                transparent 0,
                transparent calc(var(--cell-width) - 1px),
                #d1d9e6 calc(var(--cell-width) - 1px),
                #d1d9e6 var(--cell-width)
            ),
            repeating-linear-gradient(
                to bottom,
                transparent 0,
                transparent 39px,
                #e2e8f0 39px,
                #e2e8f0 40px
            );
        
        background-position: 0 0;
        pointer-events: none;
        background-size: calc(var(--cell-width) * 24) auto;
    }
    
    .interval {
        position: absolute;
        border-radius: 4px;
        overflow: hidden;
        pointer-events: auto;
        margin: 0;
        
        font-size: clamp(0.65rem, 0.8vw, 0.8rem);
    }
    
    .interval-schedule {
        cursor: pointer;
        transition: transform 0.2s, box-shadow 0.2s;
        z-index: 10;
    }
    
    .interval-schedule:hover {
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 20;
    }
    
    .interval-shadow {
        z-index: 10;
        border-left: 1px solid black;
        border-right: 1px solid black;
        border-radius: 0; 
    }

    .interval-zasvetka {
        z-index: 20;
        border-left: 1px solid black;
        border-right: 1px solid black;
        border-radius: 0; 
    }
    
    .interval-content {
        height: 100%;
        width: 100%;
        border-radius: 4px;
        border: 1px solid black;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-weight: 600;
        box-sizing: border-box;
        overflow: hidden;
    }
    
    .interval-time {
        white-space: nowrap;
        text-overflow: ellipsis;
        overflow: hidden;
        text-shadow: 0 1px 2px rgba(0,0,0,0.2);
        max-width: 100%;
    }
    
    .mode-label-container {
        position: absolute;
        left: 0;
        width: 100%;
        height: 50px;
        display: flex;
        align-items: center;
        padding: 0 0 0 1rem;
        background: white;
        font-weight: 600;
        color: #2d3748;
        z-index: 2;
    }
    
    .mode-checkbox {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        cursor: pointer;
        width: 100%;
    }
    
    .mode-checkbox input {
        margin: 0;
    }
    
    .mode-text {
        color: #2d3748;
        font-weight: 600;
        font-size: clamp(0.65rem, 0.8vw, 0.8rem);
    }
</style>
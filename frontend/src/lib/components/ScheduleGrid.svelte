<script lang="ts">
    import type {
    	RotationInterval,
    	ShadowInterval,
    	TimeInterval,
    	VkiInterval,
    	WorkMode,
    	ZasvetkaInterval
    } from '$lib/types/schedule';
    import { GridPositionUtils } from "../utils/gridPosition";

    let {
        intervals,
        shadowIntervals = [],
        zasvetkaIntervals = [],
        vkiIntervals = [],
        rotationIntervals = [],
        workModes = [],
        onModeSelect,
        getIntervalColor,
        getIntervalTitle,
        onIntervalClick,
        onIntervalDelete,
        selectedIntervalId = null,
        isEditing = false
    } = $props<{
        intervals: TimeInterval[];
        shadowIntervals?: ShadowInterval[];
        zasvetkaIntervals?: ZasvetkaInterval[];
        vkiIntervals?: VkiInterval[];
        rotationIntervals?: RotationInterval[],
        workModes?: WorkMode[];
        onModeSelect?: (modeId: number) => void;
        getIntervalColor?: (interval: TimeInterval) => string;
        getIntervalTitle?: (interval: TimeInterval) => string;
        onIntervalClick?: (interval: TimeInterval) => void;
        onIntervalDelete?: (intervalId: string) => void;
        selectedIntervalId?: string | null;
        isEditing: boolean;
    }>();

    let containerWidth = $state(0);
    let cellWidth = $derived(GridPositionUtils.calculateCellWidth(containerWidth));
    let gridContainer = $state<HTMLDivElement>();
    let selectedMode = $state<number | null>(null);
    let contextMenu = $state<ContextMenuState>({
        show: false,
        x: 0,
        y: 0,
        intervalId: ''
    });

    type ContextMenuState = {
        show: boolean;
        x: number;
        y: number;
        intervalId: string;
    };

    type PositionedInterval = {
        type: 'schedule' | 'astrocorrection';
        id: string;
        modeIndex: number;
        color: string;
        position: any;
        className: string;
        title: string;
        opacity?: number;
        zIndex?: number;
        data: TimeInterval;
    };

    type PositionedForecastInterval = {
        type: 'shadow' | 'zasvetka';
        id: string;
        modeIndex: -1;
        color: string;
        position: any;
        title: string;
        opacity: number;
        zIndex: number;
        data: ShadowInterval | ZasvetkaInterval;
    };

    type PositionedAstroInterval = {
        type: 'vki' | 'rotation';
        id: string;
        modeIndex: -1;
        color: string;
        position: any;
        title: string;
        opacity: number;
        zIndex: number;
        data: VkiInterval | RotationInterval;
    };

    type PositionedItem = PositionedInterval | PositionedForecastInterval | PositionedAstroInterval;

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
        if (contextMenu.show) {
            const handleClickOutside = () => closeContextMenu();
            document.addEventListener('click', handleClickOutside);
            return () => document.removeEventListener('click', handleClickOutside);
        }
    });

    const filteredIntervals = $derived(
        selectedMode 
            ? intervals.filter(interval => interval.mode === selectedMode)
            : []
    );

    const positionedIntervals = $derived(() => 
        getPositionedIntervals()
    );

    function updateContainerWidth() {
        if (gridContainer) {
            containerWidth = gridContainer.offsetWidth;
        }
    }

    function closeContextMenu() {
        contextMenu.show = false;
    }

    function handleIntervalClick(event: MouseEvent, interval: TimeInterval) {
        event.preventDefault();

        if (interval.isAstrocorrection) {
            return;
        }
        
        if (event.button === 0) {

            selectedMode = interval.mode;
            onModeSelect?.(interval.mode);
            onIntervalClick?.(interval);
        } else if (event.button === 2) {
            contextMenu = {
                show: true,
                x: event.clientX,
                y: event.clientY,
                intervalId: interval.id
            };
            event.preventDefault();
        }
    }

    function handleDeleteInterval() {
        if (contextMenu.intervalId) {
            onIntervalDelete?.(contextMenu.intervalId);
            closeContextMenu();
        }
    }

    function selectMode(modeId: number) {
        selectedMode = selectedMode === modeId ? null : modeId;
        onModeSelect?.(selectedMode || modeId);
    }

    function getPositionedIntervals(): PositionedItem[] {
        if (!workModes || workModes.length === 0) {
            console.warn('workModes пуст или не определен');
            return [];
        }

        const allPositionedIntervals: PositionedItem[] = [];

        intervals.forEach((interval) => {
            const modeIndex = workModes.findIndex((m: { id: number; }) => m.id === interval.mode);
            
            if (modeIndex === -1) {
                console.warn(`Mode ${interval.mode} not found for interval ${interval.id}`);
                return;
            }
            
            const color = getIntervalColor?.(interval) || interval.color;
            const title = getIntervalTitle?.(interval) || interval.title || '';
            
            const intervalType: 'schedule' | 'astrocorrection' = interval.isAstrocorrection 
                ? 'astrocorrection' 
                : 'schedule';
            
            const positionedInterval: PositionedInterval = {
                type: intervalType,
                id: interval.id,
                modeIndex,
                color,
                position: GridPositionUtils.getPositionForInterval(
                    interval.startTime, 
                    interval.endTime,   
                    modeIndex,
                    cellWidth,
                    true
                ),
                className: getIntervalClassName(interval),
                title: `${title} ${interval.startTime}-${interval.endTime}`,
                data: interval
            };
            
            allPositionedIntervals.push(positionedInterval);
        });
        
        addForecastIntervals(allPositionedIntervals, shadowIntervals, 'shadow');
        addForecastIntervals(allPositionedIntervals, zasvetkaIntervals, 'zasvetka');
        addAstroIntervals(allPositionedIntervals, vkiIntervals, 'vki');
        addAstroIntervals(allPositionedIntervals, rotationIntervals, 'rotation');
        
        return allPositionedIntervals;
    }

    function addForecastIntervals(
        targetArray: PositionedItem[],
        intervalsArray: (ShadowInterval | ZasvetkaInterval)[],
        type: 'shadow' | 'zasvetka'
    ) {
        intervalsArray.forEach(interval => {
            const title = `${interval.title || (type === 'shadow' ? 'Тень' : 'Засветка')} ${interval.startTime}-${interval.endTime}`;

            const positionedForecastInterval: PositionedForecastInterval = {
                type,
                id: interval.id,
                modeIndex: -1,
                color: interval.color,
                position: {
                    left: GridPositionUtils.getPositionForInterval(
                        interval.startTime,
                        interval.endTime,
                        0,
                        cellWidth,
                        true
                    ).left,
                    width: GridPositionUtils.getPositionForInterval(
                        interval.startTime,
                        interval.endTime,
                        0,
                        cellWidth,
                        true
                    ).width,
                    top: `0px`,
                    height: `${GridPositionUtils.ROW_HEIGHT * (workModes.length + 1)}px`
                },
                title: title,
                opacity: interval.opacity,
                zIndex: interval.zIndex,
                data: interval
            };
            
            targetArray.push(positionedForecastInterval);
        });
    }

    function addAstroIntervals(
        targetArray: PositionedItem[],
        intervalsArray: (VkiInterval | RotationInterval)[],
        type: 'vki' | 'rotation'
    ) {
        intervalsArray.forEach(interval => {
            const title = `${interval.title || (type === 'vki' ? 'ВКИ' : 'Сезонный разворот')} ${interval.startTime}-${interval.endTime}`;
            
            const positionedAstroInterval: PositionedAstroInterval = {
                type,
                id: interval.id,
                modeIndex: -1,
                color: interval.color,
                position: {
                    left: GridPositionUtils.getPositionForInterval(
                        interval.startTime,
                        interval.endTime,
                        0,
                        cellWidth,
                        true
                    ).left,
                    width: GridPositionUtils.getPositionForInterval(
                        interval.startTime,
                        interval.endTime,
                        0,
                        cellWidth,
                        true
                    ).width,
                    top: `0px`,
                    height: `${GridPositionUtils.ROW_HEIGHT * (workModes.length + 1)}px`
                },
                title: title,
                opacity: interval.opacity,
                zIndex: interval.zIndex,
                data: interval
            };
            
            targetArray.push(positionedAstroInterval);
        });
    }

    function getIntervalClassName(interval: TimeInterval): string {
        const classes = [];
        
        if (interval.isAstrocorrection) {
            classes.push('astrocorrection-interval');
        }

        if (interval.zasvetkaConflict || interval.nearZasvetka) {
            classes.push('zasvetka-conflict-interval');
        } else if (interval.hasConflict) {
            classes.push('conflict-interval');
        } else if (interval.constraintViolations?.length) {
            classes.push('constraint-violation'); 
        }

        if (interval.inShadow) {
            if (interval.willBeSavedInShadow) {
                classes.push('shadow-winner-interval');
            } else {
                classes.push('shadow-interval');
            }
        }
        
        if (interval.id === selectedIntervalId) {
            classes.push('selected-interval');
        }
        
        return classes.join(' ');
    }
</script>

<div class="schedule-grid">
    {#if workModes && workModes.length > 0}
        <div class="modes-container">
            {#each workModes as mode, i}
            {@const isAstroMode = mode.id === 9}
                <div class="mode-label-container" 
                     style="top: {1.4 * GridPositionUtils.TIME_HEIGHT + i * GridPositionUtils.ROW_HEIGHT}px">
                    <label class="mode-checkbox">
                        <input 
                            type="radio" 
                            name="workMode"
                            value={mode.id}
                            checked={selectedMode === mode.id}
                            on:change={() => selectMode(mode.id)}
                            disabled={isEditing || isAstroMode}
                        />
                        <span class="mode-text">{mode.label}</span>
                    </label>
                </div>
            {/each}
        </div>
    {/if}
    
    <div class="schedule-grid_container" bind:this={gridContainer}>
        <div class="time-scale top-scale">
            {#each GridPositionUtils.HOURS as hour}
                <div class="hour-marker" 
                     style="left: {(hour * cellWidth)}px; width: {cellWidth}px">
                    <div class="hour-label">
                        {hour.toString().padStart(2, '0')}:00
                    </div>
                </div>
            {/each}
        </div>
        
        <div class="time-scale bottom-scale">
            {#each GridPositionUtils.HOURS as hour}
                <div class="hour-marker" 
                     style="left: {(hour * cellWidth)}px; width: {cellWidth}px">
                    <div class="hour-label">
                        {hour.toString().padStart(2, '0')}:00
                    </div>
                </div>
            {/each}
        </div>

        <div class="grid-area"
             style="
                grid-template-columns: repeat(24, {cellWidth}px); 
                width: {cellWidth * 24 + 5}px;
                --cell-width: {cellWidth}px;
             ">
            {#each positionedIntervals() as item (item.id)}
                {#if item.type === 'astrocorrection'}
                    <div class="interval interval-astrocorrection {item.className}"
                        style="
                            left: {item.position.left}; 
                            width: {item.position.width}; 
                            top: {item.position.top}; 
                            height: {item.position.height};
                            background: {item.color};
                            opacity: {item.opacity || 1};
                            z-index: {item.zIndex || 5};
                        "
                        title="{item.title}">
                    </div>
                {:else if item.type === 'schedule'}
                    <div class="interval interval-schedule {item.className}"
                        style="
                            left: {item.position.left}; 
                            width: {item.position.width}; 
                            top: {item.position.top}; 
                            height: {item.position.height};
                            background: {item.color};
                            opacity: {item.opacity || 1};
                            z-index: {item.zIndex || 10};
                        "
                        title="{item.title}"
                        on:click={(e) => handleIntervalClick(e, item.data)}
                        on:contextmenu|preventDefault={(e) => handleIntervalClick(e, item.data)}>
                        <div class="interval-content" style="background: {item.color};">
                        </div>
                    </div>
                {:else if item.type === 'shadow' || item.type === 'zasvetka'}
                    <div class="interval interval-{item.type}"
                        style="
                            left: {item.position.left}; 
                            width: {item.position.width}; 
                            top: {item.position.top}; 
                            height: {item.position.height};
                            background: {item.color};
                            opacity: {item.opacity || 1};
                            z-index: {item.zIndex || 10};
                        "
                        title="{item.title}">
                    </div>
                {:else if item.type === 'vki' || item.type === 'rotation'}
                    <div class="interval interval-{item.type}"
                        style="
                            left: {item.position.left}; 
                            width: {item.position.width}; 
                            top: {item.position.top}; 
                            height: {item.position.height};
                            background: {item.color};
                            opacity: {item.opacity || 1};
                            z-index: {item.zIndex || 10};
                        "
                        title="{item.title}">
                    </div>
                {/if}
            {/each}
        </div>
    </div>

    {#if contextMenu.show}
        <div class="context-menu"
             style="position: fixed; left: {contextMenu.x}px; top: {contextMenu.y}px;"
             on:click|stopPropagation>
            <button class="delete-btn" on:click={handleDeleteInterval}>
                Удалить
            </button>
        </div>
    {/if}
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

    .interval-vki, .interval-rotation {
        cursor: default !important;
        border-radius: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
    }
    
    .interval-shadow {
        z-index: 20;
        border-left: 1px solid black;
        border-right: 1px solid black;
        border-radius: 0; 
        cursor: default !important;
    }

    .interval-zasvetka {
        z-index: 10;
        border-left: 1px solid black;
        border-right: 1px solid black;
        border-radius: 0; 
        cursor: default !important;
    }
    
    .interval-astrocorrection {
        cursor: default !important;    
        border: 1px solid black;    
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
    
    .selected-interval {
        outline: 2px solid black !important;
        z-index: 30 !important;
    }

    .context-menu {
        background: white;
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        padding: 0;
        z-index: 1000;
    }

    .delete-btn {
        padding: 8px;
        background: #ffffff;
        color: rgb(255, 0, 0);
        border: 1px solid red;
        border-radius: 4px;
        cursor: pointer;
        font-size: 14px;
        font-weight: bold;
        transition: background 0.2s;
    }

    .delete-btn:hover {
        background: #ff0000;
        color: white;
    }

</style>
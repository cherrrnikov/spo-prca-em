<script lang="ts">
    import type { TimeInterval, WorkMode } from '$lib/types/schedule';

    let { intervals, workModes } = $props<{
        intervals: TimeInterval[];
        workModes: WorkMode[];
    }>();

    const HOURS = Array.from({length: 24}, (_, i) => i);
    const CELL_WIDTH = 70;
    const ROW_HEIGHT = 50;
    const TIME_HEIGHT = 40;
    
    let selectedModes = $state(new Set<string>(workModes.map((m: { id: any; }) => m.id)));
    
    const filteredIntervals = $derived(
        intervals.filter((interval: { mode: string; }) => selectedModes.has(interval.mode))
    );

    function timeToMinutes(time: string): number {
        const [hours, minutes] = time.split(':').map(Number);
        return hours * 60 + (minutes || 0);
    }

    function minutesToPixels(minutes: number): number {
        return (minutes / 60) * CELL_WIDTH;
    }

    function getIntervalPosition(interval: TimeInterval, modeIndex: number) {
        const startMinutes = timeToMinutes(interval.startTime);
        const endMinutes = timeToMinutes(interval.endTime);
        const durationMinutes = endMinutes - startMinutes;

        return `
            left: ${minutesToPixels(startMinutes)}px;
            width: ${minutesToPixels(durationMinutes)}px;
            top: ${TIME_HEIGHT + modeIndex * ROW_HEIGHT - 10}px;
            height: ${ROW_HEIGHT - 10}px;
        `;
    }

    function getPositionedIntervals() {
        return filteredIntervals
            .map((interval: { mode: string; }) => {
                const modeIndex = workModes.findIndex(m => m.id === interval.mode);
                return {
                    ...interval,
                    modeIndex,
                    position: getIntervalPosition(interval, modeIndex)
                };
            })
            .filter((item: { modeIndex: number; }) => item.modeIndex !== -1);
    }
    
    function toggleMode(modeId: string) {
        if (selectedModes.has(modeId)) {
            selectedModes.delete(modeId);
        } else {
            selectedModes.add(modeId);
        }
    }

</script>

<div class="schedule-grid">
    <div class="modes-container">
        {#each workModes as mode, i}
            <div class="mode-label-container" style="top: {1.6 * TIME_HEIGHT + i * ROW_HEIGHT}px">
                <label class="mode-checkbox">
                    <input 
                        type="radio" 
                        on:change={() => toggleMode(mode.id)}
                    />
                    <span class="mode-text">{mode.label}</span>
                </label>
            </div>
        {/each}
    </div>
    
    <div class="schedule-grid_container">
        <!-- Верхняя шкала времени -->
        <div class="time-scale top-scale">
            {#each HOURS as hour}
                <div class="hour-marker" style="left: {hour * CELL_WIDTH}px">
                    <div class="hour-label">
                        {hour.toString().padStart(2, '0')}:00
                    </div>
                </div>
            {/each}
        </div>
        
        <div class="time-scale bottom-scale">
            {#each HOURS as hour}
                <div class="hour-marker" style="left: {hour * CELL_WIDTH}px">
                    <div class="hour-label">
                        {hour.toString().padStart(2, '0')}:00
                    </div>
                </div>
            {/each}
        </div>
        
        <div class="grid-area">
            {#each getPositionedIntervals() as item}
                <div 
                    class="interval" 
                    style={item.position}
                    title="{item.title || ''} {item.startTime}-{item.endTime}"
                >
                    <div class="interval-content" style="background: {item.color}; border-color: {item.color}">
                        <div class="interval-time">
                            {item.startTime} - {item.endTime}
                        </div>
                    </div>
                </div>
            {/each}
        </div>
    </div>
</div>

<style>
    .schedule-grid {
        display: flex;
        position: relative;
        width: 100%;
        min-height: 635px;
        background: white;
        padding-right: 40px;
    }
    
    .time-scale {
        position: absolute;
        height: 40px;
        width: 100%;
        background: #f8fafc;
        z-index: 20;
    }
    
    .top-scale {
        top: 0;
        /* border-bottom: 2px solid #4a5568; */
    }
    
    .bottom-scale {
        bottom: 0;
        /* border-top: 2px solid #4a5568; */
    }
    
    .hour-marker {
        position: absolute;
    }
    
    .hour-label {
        position: absolute;
        top: 0px;
        left: -20px;
        width: 40px;
        text-align: center;
        font-size: 0.8rem;
        color: #4a5568;
        font-weight: 500;
    }
    
    .top-scale .hour-label {
        top: 15px;
    }
    
    .modes-container {
        position: relative;
        width: 9%;
        min-height: 340px;
    }

    .schedule-grid_container {
        position: relative;
        width: 91%;
        min-height: 340px;
    }
    
    .grid-area {
        position: absolute;
        width: 98.4%;
        top: 40px;
        left: 0;
        right: 0;
        border: 2px solid #4a5568;
        
        display: grid;
        grid-template-columns: repeat(24, 70px);
        grid-template-rows: repeat(11, 50px);
        
        background-image: 
            repeating-linear-gradient(
                to right,
                transparent 0,
                transparent 69px,
                #d1d9e6 69px,
                #d1d9e6 70px
            ),
            repeating-linear-gradient(
                to bottom,
                transparent 0,
                transparent 49px,
                #e2e8f0 49px,
                #e2e8f0 50px
            );
        
        background-size: calc(100% - 20px) calc(100% - 50px);
        background-position: 0 0;
        pointer-events: none;
    }
    
    .interval {
        position: absolute;
        border-radius: 4px;
        cursor: pointer;
        z-index: 10;
        transition: transform 0.2s, box-shadow 0.2s;
        overflow: hidden;
        pointer-events: auto;
    }
    
    .interval:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 20;
    }
    
    .interval-content {
        height: 100%;
        width: 100%;
        border-radius: 4px;
        border: 2px solid;
        padding: 0.5rem;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-size: 0.85rem;
        font-weight: 600;
        box-sizing: border-box;
    }
    
    .interval-time {
        white-space: nowrap;
        text-shadow: 0 1px 2px rgba(0,0,0,0.2);
    }
    
    .mode-label-container {
        position: absolute;
        left: 0;
        width: 100%;
        height: 50px;
        display: flex;
        align-items: center;
        padding: 0 1rem;
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
        font-size: 0.9rem;
        color: #2d3748;
        font-weight: 600;
    }
</style>
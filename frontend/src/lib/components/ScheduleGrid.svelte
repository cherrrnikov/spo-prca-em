<script lang="ts">
    import type { TimeInterval, WorkMode } from '$lib/types/schedule';

    let { intervals, workModes } = $props<{
        intervals: TimeInterval[];
        workModes: WorkMode[];
    }>();

    const HOURS = Array.from({length: 24}, (_, i) => i);
    const CELL_WIDTH = 70;
    const ROW_HEIGHT = 50;
    const TIME_HEIGHT = 50;
    
    let selectedModes = $state(new Set<string>(workModes.map(m => m.id)));
    
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

    function getIntervalStyle(interval: TimeInterval, modeIndex: number) {
        const startMinutes = timeToMinutes(interval.startTime);
        const endMinutes = timeToMinutes(interval.endTime);
        const durationMinutes = endMinutes - startMinutes;

        return `
            left: ${minutesToPixels(startMinutes)}px;
            width: ${minutesToPixels(durationMinutes)}px;
            top: ${TIME_HEIGHT + modeIndex * ROW_HEIGHT + 10}px;
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
            <div class="mode-label-container" style="top: {TIME_HEIGHT + i * ROW_HEIGHT}px">
                <label class="mode-checkbox">
                    <input 
                        type="checkbox" 
                        checked={selectedModes.has(mode.id)}
                        on:change={() => toggleMode(mode.id)}
                    />
                    <span class="mode-text">{mode.label}</span>
                </label>
            </div>
        {/each}
        
    </div>
    <div class="schedule-grid_container">
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
        
        <div class="vertical-dashed-lines">
            {#each HOURS.slice(1, 24) as hour}
                <div 
                    class="dashed-line" 
                    style="left: {hour * CELL_WIDTH}px"
                ></div>
            {/each}
        </div>
        {#each workModes as mode, i}
            <div class="mode-line" style="top: {TIME_HEIGHT + i * ROW_HEIGHT + 30}px"></div>
        {/each}
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

<style>
    .schedule-grid {
        display: flex;
        position: relative;
        width: 100%; /* 120 + 24*72 = 120 + 1728 = 1848 */
        min-height: 620px; /* 40 + 4*60 = 280px (для 4 режимов) */
        background: white;
        padding-right: 40px;
    }
    
    .time-scale {
        position: absolute;
        height: 40px;
        width: 100%;
        background: #f8fafc;
    }
    
    .top-scale {
        top: 0;
        border-bottom: 2px solid #4a5568;
    }
    
    .bottom-scale {
        bottom: 0;
        border-top: 2px solid #4a5568;
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
    
    .vertical-dashed-lines {
        position: absolute;
        top: 40px;
        bottom: 40px;
        pointer-events: none;
    }
    
    .dashed-line {
        position: absolute;
        top: 0;
        bottom: 0;
        width: 1px;
        border-left: 1px dashed #d1d9e6;
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
    
    .mode-line {
        position: absolute;
        height: 1px;
        left: 0;
        width: 100%;
        background: #e2e8f0;
        z-index: 1;
    }
    
    .interval {
        position: absolute;
        border-radius: 4px;
        cursor: pointer;
        z-index: 10;
        transition: transform 0.2s, box-shadow 0.2s;
        overflow: hidden;
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
    
    .solid-line {
        position: absolute;
        top: 0;
        bottom: 0;
        width: 1px;
        background: #f1f5f9;
    }
    
    .solid-line:nth-child(4n) {
        background: #e2e8f0;
    }
</style>
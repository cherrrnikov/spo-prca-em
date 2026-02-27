<script lang="ts">
    import type { ProgramsListItem } from '$lib/types/analysis';
    import { TimeUtils } from '$lib/utils/time';
    
    let {
        programs = [],
        activeId = null,
        onSelect,
        onExitAnalysis,
        onDelete
    } = $props<{
        programs: ProgramsListItem[];
        activeId: string | null;
        onSelect: (id: string) => void;
        onExitAnalysis: () => void;
        onDelete?: (id: string) => void;
    }>();
    
    let isOpen = $state(false);

    $effect(() => {
        console.log("=== СПИСОК ПРЦА В АНАЛИЗЕ ===");
        console.log(`Всего ПРЦА: ${programs.length}`);
        programs.forEach((p, index) => {
            console.log(`${index + 1}. ${p.name} (ID: ${p.id}) - ${p.intervals.length} интервалов`);
        });
    });
</script>

<div class="programs-selector">
    <div class="selector-header">
        <div class="selector-title">
            <h3>Анализ ПРЦА</h3>
        </div>
        <div class="selector-actions">
            <button 
                class="exit-button" 
                on:click={onExitAnalysis}
                title="Выйти из режима анализа"
            >
                ✕
            </button>
            <button 
                class="toggle-button"
                on:click={() => isOpen = !isOpen}
                aria-label={isOpen ? 'Свернуть' : 'Развернуть'}
            >
                {isOpen ? '▼' : '▶'}
            </button>
        </div>
    </div>
    
    {#if isOpen}
        <div class="programs-list">
            {#each programs as program}
                <label class="program-item {program.id === activeId ? 'active' : ''}">
                    <input
                        type="radio"
                        name="program-select"
                        value={program.id}
                        checked={program.id === activeId}
                        on:change={() => onSelect(program.id)}
                    />
                    <div class="program-info">
                        <span class="program-name">{program.name}</span>
                        <span class="program-date">{TimeUtils.formatDate(program.date)}</span>
                    </div>
                </label>
                {#if onDelete}
                    <button 
                        class="delete-program" 
                        on:click={() => onDelete(program.id)}
                        title="Удалить из анализа"
                    >
                        Удалить
                    </button>
                {/if}
            {/each}
        </div>
    {/if}
</div>

<style>
    .programs-selector {
        background: white;
        border-radius: 8px;
        border: 1px solid #e2e8f0;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        margin-bottom: 1rem;
        overflow: hidden;
    }
    
    .selector-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.75rem 1rem;
        background: #f8fafc;
        border-bottom: 1px solid #e2e8f0;
    }
    
    .selector-title {
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    
    .selector-title h3 {
        margin: 0;
        font-size: 0.95rem;
        font-weight: 600;
        color: #2d3748;
    }
    
    .icon {
        font-size: 1.1rem;
    }
    
    .selector-actions {
        display: flex;
        gap: 0.5rem;
    }
    
    .exit-button {
        background: #e53e3e;
        color: white;
        border: none;
        width: 24px;
        height: 24px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 0.8rem;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.2s;
    }
    
    .exit-button:hover {
        background: #c53030;
    }
    
    .toggle-button {
        background: #edf2f7;
        border: none;
        width: 24px;
        height: 24px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 0.8rem;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.2s;
    }
    
    .toggle-button:hover {
        background: #e2e8f0;
    }
    
    .programs-list {
        padding: 0.5rem;
        max-height: 200px;
        overflow-y: auto;
    }
    
    .program-item {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 0.75rem;
        border-radius: 6px;
        cursor: pointer;
        transition: background 0.2s;
        border: 1px solid transparent;
    }
    
    .program-item:hover {
        background: #f7fafc;
    }
    
    .program-item.active {
        background: #ebf8ff;
        border-color: #4299e1;
    }
    
    .program-item input[type="radio"] {
        width: 18px;
        height: 18px;
        cursor: pointer;
    }
    
    .program-info {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
    }
    
    .program-name {
        font-weight: 600;
        color: #2d3748;
        font-size: 0.9rem;
    }
    
    .program-date {
        font-size: 0.8rem;
        color: #718096;
    }

    .program-item-wrapper {
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    
    .program-item {
        flex: 1;
    }
    
    .delete-program {
        background: none;
        border: none;
        cursor: pointer;
        padding: 0.25rem 0.5rem;
        font-size: 1rem;
        opacity: 0.6;
        transition: opacity 0.2s;
        border-radius: 4px;
    }
    
    .delete-program:hover {
        opacity: 1;
        background: #fee2e2;
    }
</style>
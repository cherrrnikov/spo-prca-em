<script lang="ts">
    import type { ProgramsListItem } from '$lib/types/analysis';
    
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
    
    let isOpen = $state(true);

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
            <h3>Анализ ПРЦА ({programs.length})</h3>
        </div>
        <div class="selector-actions">
            <button 
                class="exit-button" 
                on:click={onExitAnalysis}
                title="Выйти из режима анализа"
            >
                ✕
            </button>
            <!-- <button 
                class="toggle-button"
                on:click={() => isOpen = !isOpen}
                aria-label={isOpen ? 'Свернуть' : 'Развернуть'}
            >
                {isOpen ? '▼' : '▶'}
            </button> -->
        </div>
    </div>
    
    {#if isOpen}
        <div class="programs-grid">
            {#each programs as program}
                <div class="program-card {program.id === activeId ? 'active' : ''}">
                    <input
                        type="radio"
                        name="program-select"
                        id={program.id}
                        value={program.id}
                        checked={program.id === activeId}
                        on:change={() => onSelect(program.id)}
                        class="program-radio"
                    />
                    <label for={program.id} class="program-label">
                        <div class="program-info">
                            <span class="program-name">{program.name}</span>
                        </div>
                    </label>
                    {#if onDelete}
                        <button 
                            class="delete-program" 
                            on:click={() => onDelete(program.id)}
                            title="Удалить из анализа"
                        >
                            ✕
                        </button>
                    {/if}
                </div>
            {/each}
        </div>
    {/if}
</div>

<style>
    .programs-selector {
        overflow: hidden;
        width: 40%;
    }
    
    .selector-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 0.5rem;
    }
    
    .selector-title h3 {
        margin: 0;
        font-size: 1rem;
        font-weight: bold;
        color: #2d3748;
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
    
    /* .toggle-button {
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
    } */
    
    .programs-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
        gap: 0.75rem;
        /* padding: 1rem; */
        max-height: 300px;
        overflow-y: auto;
    }
    
    .program-card {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.5rem;
        border-radius: 6px;
        border: 1px solid #e2e8f0;
        background: #f8fafc;
        transition: all 0.2s;
        min-width: 0; /* для корректного переноса текста */
    }
    
    .program-card.active {
        border-color: #4299e1;
        background: #ebf8ff;
        box-shadow: 0 0 0 2px rgba(66, 153, 225, 0.2);
    }
    
    .program-radio {
        width: 16px;
        height: 16px;
        cursor: pointer;
        flex-shrink: 0;
    }
    
    .program-label {
        flex: 1;
        cursor: pointer;
        min-width: 0; /* для переноса */
    }
    
    .program-info {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
        overflow: hidden;
    }
    
    .program-name {
        font-weight: 600;
        color: #2d3748;
        font-size: 0.85rem;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }
    
    .program-date {
        font-size: 0.75rem;
        color: #718096;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }
    
    .delete-program {
        background: none;
        border: none;
        cursor: pointer;
        width: 20px;
        height: 20px;
        font-size: 0.8rem;
        opacity: 0.6;
        transition: opacity 0.2s;
        border-radius: 4px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        color: #e53e3e;
    }
    
    .delete-program:hover {
        opacity: 1;
        background: #fee2e2;
    }
</style>
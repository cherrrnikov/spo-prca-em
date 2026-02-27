<script lang="ts">
    import type { AnalysisModalState } from '$lib/types/analysis';
    
    let {
        modalData,
        onClose,
        onCreate
    } = $props<{
        modalData: AnalysisModalState;
        onClose: () => void;
        onCreate: (startDate: string, endDate: string) => Promise<void>;
    }>();
    
    let startDate = $state(modalData.startDate);
    let endDate = $state(modalData.endDate);
    
    function validateDates(): boolean {
        if (!startDate || !endDate) {
            alert('Выберите начальную и конечную даты');
            return false;
        }
        
        if (new Date(startDate) > new Date(endDate)) {
            alert('Дата начала не может быть позже даты окончания');
            return false;
        }
        
        return true;
    }
    
    async function handleCreate() {
        if (!validateDates()) return;
        await onCreate(startDate, endDate);
    }
</script>

{#if modalData.isOpen}
    <div class="modal-overlay" on:click|self={onClose}>
        <div class="modal-container">
            <div class="modal-header">
                <h2>Создание анализа</h2>
                <button class="close-button" on:click={onClose}>×</button>
            </div>
            
            <div class="modal-content">
                <p class="description">
                    Выберите диапазон дат для копирования текущей ПРЦА.
                    Для каждой даты будут загружены актуальные данные (тени, засветки, ВКИ).
                </p>
                
                <div class="date-range">
                    <div class="form-group">
                        <label>Дата начала:</label>
                        <input 
                            type="date" 
                            bind:value={startDate}
                            class="date-input"
                        />
                    </div>
                    
                    <div class="form-group">
                        <label>Дата окончания:</label>
                        <input 
                            type="date" 
                            bind:value={endDate}
                            class="date-input"
                        />
                    </div>
                </div>
            </div>
            
            <div class="modal-actions">
                <button 
                    class="btn-create"
                    on:click={handleCreate}
                    disabled={modalData.isLoading}
                >
                    {modalData.isLoading ? 'Создание...' : 'Создать анализ'}
                </button>
                <button 
                    class="btn-cancel"
                    on:click={onClose}
                    disabled={modalData.isLoading}
                >
                    Отмена
                </button>
            </div>
        </div>
    </div>
{/if}

<style>
    .modal-overlay {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1100;
        backdrop-filter: blur(2px);
    }
    
    .modal-container {
        background: white;
        border-radius: 8px;
        width: 500px;
        max-width: 90vw;
        overflow: hidden;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
    }
    
    .modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem 1.5rem;
        background: linear-gradient(135deg, #667eea, #764ba2);
        color: white;
    }
    
    .modal-header h2 {
        margin: 0;
        font-size: 1.25rem;
        font-weight: 600;
    }
    
    .close-button {
        background: rgba(255, 255, 255, 0.2);
        border: none;
        color: white;
        font-size: 1.5rem;
        cursor: pointer;
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 4px;
        transition: background 0.2s;
    }
    
    .close-button:hover {
        background: rgba(255, 255, 255, 0.3);
    }
    
    .modal-content {
        padding: 1.5rem;
    }
    
    .description {
        color: #4a5568;
        font-size: 0.95rem;
        margin-bottom: 1.5rem;
        line-height: 1.5;
    }
    
    .date-range {
        display: flex;
        gap: 1rem;
        flex-wrap: wrap;
    }
    
    .form-group {
        flex: 1;
        min-width: 200px;
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
    }
    
    .form-group label {
        font-weight: 600;
        color: #2d3748;
        font-size: 0.9rem;
    }
    
    .date-input {
        padding: 0.75rem;
        border: 2px solid #e2e8f0;
        border-radius: 6px;
        font-size: 1rem;
        transition: border-color 0.2s;
    }
    
    .date-input:focus {
        outline: none;
        border-color: #667eea;
    }
    
    .modal-actions {
        display: flex;
        gap: 0.75rem;
        padding: 1.5rem;
        background: #f8fafc;
        border-top: 1px solid #e2e8f0;
    }
    
    .btn-create {
        flex: 1;
        padding: 0.875rem;
        background: linear-gradient(135deg, #48bb78, #38a169);
        color: white;
        border: none;
        border-radius: 6px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
    }
    
    .btn-create:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(72, 187, 120, 0.3);
    }
    
    .btn-cancel {
        flex: 1;
        padding: 0.875rem;
        background: #e2e8f0;
        color: #4a5568;
        border: none;
        border-radius: 6px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
    }
    
    .btn-cancel:hover:not(:disabled) {
        background: #cbd5e0;
    }
    
    button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }
</style>
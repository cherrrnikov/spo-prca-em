<script lang="ts">
    import type {
    	MsuGsType,
    	ScheduleStatus,
    	ShootingMode
    } from '../types';

    let {
        scheduleStatus = $bindable('main'),
        selectedDate = $bindable(''),
        selectedTime = $bindable(''),
        shootingMode = $bindable('default'),
        msuGsType = $bindable('msu_gs_1'),
        isLoading = false,
        onSubmit,
        onCancel
    } = $props<{
        scheduleStatus?: ScheduleStatus;
        selectedDate?: string;
        selectedTime?: string;
        shootingMode?: ShootingMode;
        msuGsType?: MsuGsType;
        isLoading?: boolean;
        onSubmit: () => Promise<void>;
        onCancel: () => void;
    }>();

    function handleSubmit() {
        onSubmit();
    }
</script>

<div class="creation-form">
    <div class="creation-header">
        <h3 class="creation-title">Создание ПРЦА по данным оператора</h3>
    </div>
    <div class="form-block">
         <div class="form-group">
        <label class="form-label">Статус ПРЦА:</label>
        <div class="radio-group">
            <label class="radio-label">
                <input 
                    type="radio" 
                    name="scheduleStatus"
                    value="main"
                    checked={scheduleStatus === 'main'}
                    on:change={(e) => scheduleStatus = e.target.value as ScheduleStatus}
                />
                <span>Основная</span>
            </label>
            <label class="radio-label">
                <input 
                    type="radio" 
                    name="scheduleStatus"
                    value="corrective"
                    checked={scheduleStatus === 'corrective'}
                    on:change={(e) => scheduleStatus = e.target.value as ScheduleStatus}
                />
                <span>Корректирующая</span>
            </label>
        </div>
    </div>

    <div class="form-group">
        <label class="form-label">Дата планирования:</label>
        <div class="datetime-inputs">
            <input 
                type="date" 
                value={selectedDate}
                on:input={(e) => selectedDate = e.target.value}
                class="date-input"
            />
            <input 
                type="time" 
                value={selectedTime}
                on:input={(e) => selectedTime = e.target.value}
                class="time-input"
            />
        </div>
    </div>

    <div class="form-group">
        <label class="form-label">Штатные съемки:</label>
        <div class="radio-group">
            <label class="radio-label">
                <input 
                    type="radio" 
                    name="shootingMode"
                    value="default"
                    checked={shootingMode === 'default'}
                    on:change={(e) => shootingMode = e.target.value as ShootingMode}
                />
                <span>По умолчанию</span>
            </label>
            <label class="radio-label">
                <input 
                    type="radio" 
                    name="shootingMode"
                    value="no_shooting"
                    checked={shootingMode === 'no_shooting'}
                    on:change={(e) => shootingMode = e.target.value as ShootingMode}
                />
                <span>Без съемки</span>
            </label>
        </div>
    </div>

    <div class="form-group">
        <label class="form-label">Признак МСУ ГС:</label>
        <div class="radio-group">
            <label class="radio-label">
                <input 
                    type="radio" 
                    name="msuGsType"
                    value="msu_gs_1"
                    checked={msuGsType === 'msu_gs_1'}
                    on:change={(e) => msuGsType = e.target.value as MsuGsType}
                />
                <span>Комплект МСУ ГС 1</span>
            </label>
            <label class="radio-label">
                <input 
                    type="radio" 
                    name="msuGsType"
                    value="msu_gs_2"
                    checked={msuGsType === 'msu_gs_2'}
                    on:change={(e) => msuGsType = e.target.value as MsuGsType}
                />
                <span>Комплект МСУ ГС 2</span>
            </label>
        </div>
    </div>

    <div class="form-actions">
        <button 
            on:click={handleSubmit}
            class="create-button"
            disabled={isLoading || !selectedDate}
        >
            {isLoading ? 'Загрузка...' : 'Загрузить данные'}
        </button>
        <button 
            on:click={onCancel}
            class="cancel-button"
        >
            Отмена
        </button>
    </div>
    </div>
</div>

<style>
    .creation-form {
        display: flex;
        flex-direction: column;
        flex-wrap: wrap;
    }

    .form-block {
        display: flex;
        align-items: center;
        gap: 2rem;
        flex-wrap: wrap;
    }
    
    .creation-title {
        font-size: 1.1rem;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 0.5rem;
    }

    .form-group {
        display: flex;
        flex-direction: column;
        gap: 0.375rem;
        min-width: 180px;
    }

    .form-label {
        font-size: 0.85rem;
        font-weight: 500;
        color: #4a5568;
        white-space: nowrap;
    }

    .radio-group {
        display: flex;
        gap: 1rem;
    }

    .radio-label {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        cursor: pointer;
        font-size: 0.85rem;
        color: #2d3748;
    }

    .datetime-inputs {
        display: flex;
        gap: 0.5rem;
    }

    .date-input,
    .time-input {
        padding: 0.375rem 0.5rem;
        border: 1px solid #cbd5e0;
        border-radius: 4px;
        font-size: 0.85rem;
        background: white;
    }

    .form-actions {
        display: flex;
        gap: 0.75rem;
        margin-left: auto;
        align-items: center;
    }

    .create-button {
        background: #4299e1;
        color: white;
        border: none;
        padding: 0.5rem 1.25rem;
        border-radius: 4px;
        cursor: pointer;
        font-size: 0.85rem;
        font-weight: 500;
        transition: background 0.2s;
    }

    .create-button:hover:not(:disabled) {
        background: #3182ce;
    }

    .create-button:disabled {
        background: #a0aec0;
        cursor: not-allowed;
    }

    .cancel-button {
        background: #e2e8f0;
        color: #4a5568;
        border: none;
        padding: 0.5rem 1.25rem;
        border-radius: 4px;
        cursor: pointer;
        font-size: 0.85rem;
        font-weight: 500;
        transition: background 0.2s;
    }

    .cancel-button:hover {
        background: #cbd5e0;
    }
</style>
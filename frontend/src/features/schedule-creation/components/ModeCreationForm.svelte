<script lang="ts">
    import {
    	CUSTOMER_CODES,
    	MODE_ID_TO_CODE,
    	PPI_LIST,
    	ZG_OPTIONS
    } from "$lib/constants/schedule";
    import type { ModeCreationForm, TimeInterval, TsMsuConfig } from "$lib/types";
    import { IntervalValidationService } from "$lib/utils/intervalValidation";
    import { onMount } from "svelte";
    import { ScheduleConverterService } from "../../services/data/scheduleConverter.service";
    import { ModeDurationService } from "../../services/utils/modeDuration.service";
    import TsCheckboxGroup from "./TsCheckboxGroup.svelte";

    const customerCodes = CUSTOMER_CODES;
    const zgOptions = ZG_OPTIONS;
    const ppiList = PPI_LIST;
    const modeIdToCode = MODE_ID_TO_CODE;

    let {
        selectedMode,
        onSubmit,
        onCancel,
        editingInterval = null,
        onUpdate
    } = $props<{
        selectedMode: number;
        onSubmit: (data: ModeCreationForm) => void;
        onCancel: () => void;
        editingInterval?: TimeInterval | null;
        onUpdate?: (data: ModeCreationForm) => void;
    }>();

    const isPriorityInShadow = $derived(
        editingInterval?.inShadow && 
        editingInterval?.willBeSavedInShadow === true &&
        editingInterval?.shadowPriority !== undefined &&
        editingInterval.shadowPriority < 0.1  // минимальный приоритет (ближе всего к центру)
    );

    let modeDurations = $state<Record<string, number>>({});
    let localFormData = $state<ModeCreationForm>(getInitialFormData());

    const isEditMode = $derived(!!editingInterval);

    onMount(async () => {
        modeDurations = await ModeDurationService.loadModeDurations();
    });

    $effect(() => {
        if (editingInterval) {
            fillFormFromInterval(editingInterval);
        } else if (selectedMode && selectedMode !== localFormData.modeType) {
            resetFormForNewMode(selectedMode);
        }
    });

    function getInitialFormData(): ModeCreationForm {
        return {
            modeType: null,
            ppiNum: 1,
            duration: 300,
            customerCode: 1,
            startTime: '10:00:00',
            msu1Config: getDefaultMsuConfig(),
            msu2Config: getDefaultMsuConfig(),
            kvdConfig: {
                prMsu: 0,
                prBssd: 0,
                prZg: 0
            },
            nOna: 1
        };
    }

    function getDefaultMsuConfig(): TsMsuConfig {
        return ScheduleConverterService.getDefaultMsuConfig();
    }

    function fillFormFromInterval(interval: TimeInterval) {
        localFormData.modeType = interval.mode;
        localFormData.ppiNum = interval.ppi || 1;
        localFormData.duration = interval.dlit || 300;
        localFormData.startTime = interval.startTime;
        localFormData.customerCode = interval.customerCode || 1;
        
        if (interval.mode === 7) {
            localFormData.kvdConfig = interval.kvdConfig 
                ? { ...interval.kvdConfig }
                : { prMsu: 0, prBssd: 0, prZg: 0 };
        } else if (interval.mode === 6) {
            localFormData.nOna = interval.nOna || 1;
        } else if (interval.mode === 8) {
            localFormData.msu1Config = interval.msu1Config || getDefaultMsuConfig();
            localFormData.msu2Config = interval.msu2Config || getDefaultMsuConfig();
        }
    }

    function resetFormForNewMode(modeId: number) {
        const newFormData = getInitialFormData();
        newFormData.modeType = modeId;
        newFormData.duration = ModeDurationService.getDurationForMode(modeId, modeDurations, modeIdToCode);
        
        if (modeId === 6) {
            newFormData.nOna = 1;
        }

        localFormData = newFormData;
        console.log('Новая запись:', localFormData);
    }

    function handleSubmit() {
        if (!validateForm()) {
            return;
        }

        const dataToSubmit = prepareSubmitData();
        console.log('Отправляемые данные:', dataToSubmit);
        
        if (isEditMode) {
            onUpdate?.(dataToSubmit);
        } else {
            onSubmit(dataToSubmit);
            resetForm();
        }
    }

    function validateForm(): boolean {
        if (!localFormData.startTime || localFormData.duration <= 0) {
            alert('Укажите время начала и длительность');
            return false;
        }

        const validation = IntervalValidationService.validateTimeInput(
            localFormData.startTime, 
            localFormData.duration
        );
        
        if (!validation.isValid) {
            alert(validation.message);
            return false;
        }
        
        return true;
    }

    function handleDurationChange() {
        if (localFormData.startTime && localFormData.duration > 0) {
            const validation = IntervalValidationService.validateTimeInput(
                localFormData.startTime, 
                localFormData.duration
            );
            
            if (!validation.isValid) {
                console.error('Некорректная длительность:', validation.message);
            }
        }
    }

    function prepareSubmitData(): ModeCreationForm {
        const submitData: ModeCreationForm = {
            ...localFormData,
            msu1Config: { ...localFormData.msu1Config },
            msu2Config: { ...localFormData.msu2Config },
            kvdConfig: localFormData.kvdConfig ? { ...localFormData.kvdConfig } : {
                prMsu: 0,
                prBssd: 0,
                prZg: 0
            }
        };
        
        if (localFormData.modeType === 6) {
            submitData.nOna = localFormData.nOna || 1;
        }
        
        return submitData;
    }

    function resetForm() {
        localFormData = getInitialFormData();
        
        if (selectedMode) {
            localFormData.modeType = selectedMode;
            localFormData.duration = ModeDurationService.getDurationForMode(
                selectedMode, 
                modeDurations, 
                modeIdToCode
            );

            if (selectedMode === 6) {
                localFormData.nOna = 1;
            }
        }
    }

    function handleMsuConfigUpdate(msu: 'msu1' | 'msu2', config: TsMsuConfig) {
        if (msu === 'msu1') {
            localFormData.msu1Config = config;
        } else {
            localFormData.msu2Config = config;
        }
    }
</script>

<div class="mode-creation-form">
    <div class="form-header">
        <h3>{isEditMode ? 'Редактирование записи' : 'Добавление новой записи'}</h3>
    </div>

    <div class="form-content">
        <div class="form-section">
            <span class="form-section_title">Пункт приёма информации (ППИ)</span>
            <div class="form-group">
                <select bind:value={localFormData.ppiNum}>
                    {#each ppiList as ppi}
                        <option value={ppi.numPpi}>{ppi.name}</option>
                    {/each}
                </select>
            </div>
        </div>

        {#if selectedMode === 7}
            <div class="form-section">
                <div class="kvd-config-grid">
                    <div class="form-group">
                        <label>Комплект МСУ:</label>
                        <div class="radio-group">
                            <label class="radio-label">
                                <input 
                                    type="radio"
                                    name="kvd-msu"
                                    value="0"
                                    checked={localFormData.kvdConfig.prMsu === 0}
                                    on:change={() => localFormData.kvdConfig.prMsu = 0}
                                />
                                <span>МСУ-1</span>
                            </label>
                            <label class="radio-label">
                                <input 
                                    type="radio"
                                    name="kvd-msu"
                                    value="1"
                                    checked={localFormData.kvdConfig.prMsu === 1}
                                    on:change={() => localFormData.kvdConfig.prMsu = 1}
                                />
                                <span>МСУ-2</span>
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label>БССД:</label>
                        <div class="radio-group">
                            <label class="radio-label">
                                <input 
                                    type="radio"
                                    name="kvd-bssd"
                                    value="0"
                                    checked={localFormData.kvdConfig.prBssd === 0}
                                    on:change={() => localFormData.kvdConfig.prBssd = 0}
                                />
                                <span>БССД1</span>
                            </label>
                            <label class="radio-label">
                                <input 
                                    type="radio"
                                    name="kvd-bssd"
                                    value="1"
                                    checked={localFormData.kvdConfig.prBssd === 1}
                                    on:change={() => localFormData.kvdConfig.prBssd = 1}
                                />
                                <span>БССД2</span>
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label>ЗГ:</label>
                        <select bind:value={localFormData.kvdConfig.prZg}>
                            {#each zgOptions as zg}
                                <option value={zg.value}>{zg.label}</option>
                            {/each}
                        </select>
                    </div>
                </div>
            </div>

        {:else if selectedMode === 8}
            <div class="form-section">
                <span class="form-section_title">Комплект МСУ-ГС 1</span>
                <TsCheckboxGroup 
                    msu="msu1"
                    config={localFormData.msu1Config}
                    onUpdate={(config) => handleMsuConfigUpdate('msu1', config)}
                    disableVd={isPriorityInShadow}
                />
            </div>

            <div class="form-section">
                <span class="form-section_title">Комплект МСУ-ГС 2</span>
                <TsCheckboxGroup 
                    msu="msu2"
                    config={localFormData.msu2Config}
                    onUpdate={(config) => handleMsuConfigUpdate('msu2', config)}
                    disableVd={isPriorityInShadow}
                />
            </div>
        {:else if selectedMode === 6}
            <div class="form-section">
                <div class="kvd-config-grid">
                    <div class="form-group">
                        <label>Номер антенны:</label>
                        <div class="radio-group">  
                            <label class="radio-label">
                                <input 
                                    type="radio"
                                    name="ona-antenna"
                                    value="1"
                                    checked={localFormData.nOna === 1}
                                    on:change={() => localFormData.nOna = 1}
                                />
                                <span>ОНА1</span>
                            </label>
                            <label class="radio-label">
                                <input 
                                    type="radio"
                                    name="ona-antenna"
                                    value="2"
                                    checked={localFormData.nOna === 2}
                                    on:change={() => localFormData.nOna = 2}
                                />
                                <span>ОНА2</span>
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        {/if}

        <div class="form-section">
            <div class="form-grid">
                <div class="form-group">
                    <label>Код заказчика:</label>
                    <select bind:value={localFormData.customerCode}>
                        {#each customerCodes as code}
                            <option value={code.value}>{code.label}</option>
                        {/each}
                    </select>
                </div>

                <div class="form-group">
                    <label>Время начала:</label>
                    <input 
                        type="time" 
                        bind:value={localFormData.startTime}
                        step="1"
                    />
                </div>
                
                <div class="form-group">
                    <label>Длительность (сек):</label>
                    <input 
                        type="number" 
                        bind:value={localFormData.duration}
                        min="60"
                        step="60"
                        disabled={selectedMode === 8}
                        on:change={handleDurationChange}
                    />
                </div>
            </div>
        </div>  
    </div>

    <div class="form-actions">
        <button on:click={handleSubmit} class="btn-submit">
            {isEditMode ? 'Сохранить изменения' : 'Добавить режим'}
        </button>
        {#if isEditMode}
            <button on:click={onCancel} class="btn-cancel">
                Отменить редактирование
            </button>
        {/if}
    </div>
</div>

<style>
    .mode-creation-form {
        border-radius: 8px;
        display: flex;
        flex-direction: column;
    }

    .form-header {
        margin-bottom: 0.5rem;
    }

    .form-header h3 {
        margin: 0;
        font-size: 1.25rem;
        color: #2d3748;
    }

    .form-content {
        display: flex;
        /* flex-direction: column; */
        gap: 1.5rem;
    }

    .form-section {
        display: flex;
        flex-direction: column;
    }
    .form-section_title {
        margin-bottom: 0.5rem;
    }
    .form-section span {
        font-size: 0.875rem;
        color: #4a5568;
        font-weight: 600;
    }

    .form-grid,
    .kvd-config-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1rem;
    }

    .form-group {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
    }

    .form-group label {
        font-size: 0.875rem;
        font-weight: 500;
        color: #4a5568;
    }

    .form-group select,
    .form-group input {
        padding: 0.5rem;
        border: 1px solid #cbd5e0;
        border-radius: 4px;
        font-size: 0.875rem;
        width: 100%;
    }

    .radio-group {
        display: flex;
        gap: 0.5rem;
    }

    .radio-label {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        cursor: pointer;
        font-size: 0.875rem;
    }

    .checkbox-group {
        display: flex;
        flex-direction: column;
    }

    .checkbox-label {
        display: flex;
        align-items: center;
        gap: 0.3rem;
        cursor: pointer;
        font-size: 0.875rem;
    }

    .form-actions {
        display: flex;
        gap: 0.75rem;
        padding-top: 1rem;
        margin-top: auto;
    }

    .btn-submit {
        background: linear-gradient(135deg, #48bb78, #38a169);
        color: white;
        border: none;
        padding: 0.625rem 1.5rem;
        border-radius: 6px;
        cursor: pointer;
        font-weight: 600;
        font-size: 0.875rem;
        transition: transform 0.2s, box-shadow 0.2s;
    }

    .btn-submit:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(72, 187, 120, 0.3);
    }

    .btn-cancel {
        background: #e2e8f0;
        color: #4a5568;
        border: none;
        padding: 0.625rem 1.5rem;
        border-radius: 6px;
        cursor: pointer;
        font-weight: 600;
        font-size: 0.875rem;
        transition: background 0.2s;
    }

    .btn-cancel:hover {
        background: #cbd5e0;
    }
</style>
<script lang="ts">
    import type { ModeCreationForm, TimeInterval, TsMsuConfig } from "$lib/types/schedule";
    import { onMount } from "svelte";

    // Для режима TS (техническая съемка) оставляем старые чекбоксы
    let msu1Vd = $state({ 
        vd1: false, 
        vd2: false, 
        vd3: false 
    });
    
    let msu1Ik = $state({
        ik4: false, ik5: false, ik6: false, ik7: false,
        ik8: false, ik9: false, ik10: false
    });
    
    let msu2Vd = $state({ 
        vd1: false, 
        vd2: false, 
        vd3: false 
    });
    
    let msu2Ik = $state({
        ik4: false, ik5: false, ik6: false, ik7: false,
        ik8: false, ik9: false, ik10: false
    });

    let modeDurations = $state<Record<string, number>>({});

    export const customerCodes = [
        { value: 1, label: '01 - Заказчик 1'},
        { value: 2, label: '02 - Заказчик 2'},
        { value: 3, label: '03 - Заказчик 3'},
        { value: 4, label: '04 - Заказчик 4'},
        { value: 5, label: '05 - Заказчик 5'}
    ];

    const zgOptions = [
        { value: 0, label: 'ЗГ1' },
        { value: 1, label: 'ЗГ2' },
        { value: 2, label: 'ЗГ3' },
        { value: 3, label: 'ЗГ4' }
    ];

    const ppiList = [
        { id: 1, name: '0 - Обнинск', num: 1 },
        { id: 2, name: '1 - Долгопрудный', num: 2 },
        { id: 3, name: '2 - Новосибирск', num: 3 },
        { id: 4, name: '3 - Хабаровск', num: 4 },
        { id: 5, name: '4 - Байконур', num: 5 },
        { id: 6, name: '5 - Ханты-Мансийск', num: 6 },
        { id: 7, name: '6 - Железногорск', num: 7 },
        { id: 8, name: '7 - Улан-Удэ', num: 8 },
        { id: 9, name: '8 - Москва (НЦ ОМЗ)', num: 9 },
        { id: 10, name: '9 - Москва (НИЦ "Планета")', num: 10 }
    ];

    const MODE_ID_TO_CODE: Record<number, string> = {
        9: 'astr',  // Астрокоррекция
        1: 's',     // Съемки
        2: 'omi',   // ОМИ
        4: 'tnp',   // ТНП
        7: 'kvd',   // КВД
        8: 'ts',    // ТС
        6: 'ona'    // Юстировка ОНА
    };

    const MODE_NAMES: Record<number, string> = {
        9: 'Астрокоррекция',
        1: 'Съемки',
        2: 'Распр. ОМИ',
        4: 'Режимы ТНП',
        7: 'Калибр. ВД',
        8: 'Техн. съемки',
        6: 'Юстировки ОНА'
    };

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

    const isEditMode = $derived(!!editingInterval);

    let localFormData = $state<ModeCreationForm>({
        modeType: null,
        ppiNum: 1,
        duration: 300,
        customerCode: 1,
        startTime: '10:00',
        // msu1Vd: [],
        // msu2Vd: [],
        msu1Config: getDefaultMsuConfig(),
        msu2Config: getDefaultMsuConfig(),
        kvdConfig: {
            prMsu: 0,
            prBssd: 0,
            prZg: 0
        }
    });

    onMount(async () => {
        await loadModeDurations();
    });

    async function loadModeDurations() {
        try {
            const response = await fetch('http://localhost:8081/api/schedule/mode-durations');
            if (response.ok) {
                modeDurations = await response.json();
            } 
        } catch (error) {
            console.error("Ошибка загрузки длительностей:", error);
        }
    }

    $effect(() => {
        if (editingInterval) {
            console.log('Заполняем форму из интервала:', editingInterval);
            console.log('msu1Config:', editingInterval.msu1Config);
            console.log('msu2Config:', editingInterval.msu2Config);
            
            localFormData.modeType = editingInterval.mode;
            localFormData.ppiNum = editingInterval.ppi || 1;
            localFormData.duration = editingInterval.dlit || 300;
            localFormData.startTime = editingInterval.startTime;
            localFormData.customerCode = editingInterval.customerCode || 1;
            
            if (editingInterval.mode === 7) {
                if (editingInterval.kvdConfig) {
                    localFormData.kvdConfig = { ...editingInterval.kvdConfig };
                } else {
                    const hasMsu2 = editingInterval.msu2Vd && editingInterval.msu2Vd.length > 0;
                    localFormData.kvdConfig = {
                        prMsu: hasMsu2 ? 1 : 0,
                        prBssd: 0,
                        prZg: 0
                    };
                }
                            
                // // Для совместимости заполняем старые поля
                // localFormData.msu1Vd = editingInterval.msu1Vd || [];
                // localFormData.msu2Vd = editingInterval.msu2Vd || [];
                
            } else if (editingInterval.mode === 8) {
                localFormData.msu1Config = editingInterval.msu1Config || getDefaultMsuConfig();
                localFormData.msu2Config = editingInterval.msu2Config || getDefaultMsuConfig();
                
                console.log('Для ТС загружаем конфиг:', localFormData.msu1Config);
                
                msu1Vd = { 
                    vd1: localFormData.msu1Config.vd1 === 1,
                    vd2: localFormData.msu1Config.vd2 === 1,
                    vd3: localFormData.msu1Config.vd3 === 1
                };
                
                msu1Ik = {
                    ik4: localFormData.msu1Config.ik4 === 1,
                    ik5: localFormData.msu1Config.ik5 === 1,
                    ik6: localFormData.msu1Config.ik6 === 1,
                    ik7: localFormData.msu1Config.ik7 === 1,
                    ik8: localFormData.msu1Config.ik8 === 1,
                    ik9: localFormData.msu1Config.ik9 === 1,
                    ik10: localFormData.msu1Config.ik10 === 1
                };
                
                msu2Vd = { 
                    vd1: localFormData.msu2Config.vd1 === 1,
                    vd2: localFormData.msu2Config.vd2 === 1,
                    vd3: localFormData.msu2Config.vd3 === 1
                };
                
                msu2Ik = {
                    ik4: localFormData.msu2Config.ik4 === 1,
                    ik5: localFormData.msu2Config.ik5 === 1,
                    ik6: localFormData.msu2Config.ik6 === 1,
                    ik7: localFormData.msu2Config.ik7 === 1,
                    ik8: localFormData.msu2Config.ik8 === 1,
                    ik9: localFormData.msu2Config.ik9 === 1,
                    ik10: localFormData.msu2Config.ik10 === 1
                };
            } else {
                resetCheckboxes();
            }
        }
        else if (selectedMode && selectedMode !== localFormData.modeType) {
            console.log('Создаем новый интервал для режима:', selectedMode);

            resetForm();
            
            localFormData.modeType = selectedMode;
            
            const modeCode = MODE_ID_TO_CODE[selectedMode];
            if (modeCode && modeDurations[modeCode] !== undefined) {
                localFormData.duration = modeDurations[modeCode];
            }

            console.log('Новая запись:', localFormData);
        }
    });

    function getDefaultMsuConfig(): TsMsuConfig {
        return {
            prMsu: 0,
            prVdMsu: 0,
            prIkMsu: 0,
            vd1: 0,
            vd2: 0,
            vd3: 0,
            ik4: 0,
            ik5: 0,
            ik6: 0,
            ik7: 0,
            ik8: 0,
            ik9: 0,
            ik10: 0
        };
    }

    // Для режима TS
    function handleTsCheckbox(type: 'vd' | 'ik', msu: 'msu1' | 'msu2', number: number) {
        console.log(`handleTsCheckbox: ${type}, ${msu}, ${number}`);
        
        if (type === 'vd') {
            const vdKey = `vd${number}` as keyof typeof msu1Vd;
            
            if (msu === 'msu1') {
                const newValue = !msu1Vd[vdKey];
                msu1Vd[vdKey] = newValue;
                
                // Обновляем конфиг
                const configKey = `vd${number}` as keyof TsMsuConfig;
                localFormData.msu1Config[configKey] = newValue ? 1 : 0;

                const hasAnyVd = msu1Vd.vd1 || msu1Vd.vd2 || msu1Vd.vd3;
                localFormData.msu1Config.prVdMsu = hasAnyVd ? 1 : 0;
                
                const hasAnyIk = Object.values(msu1Ik).some(v => v);
                localFormData.msu1Config.prMsu = (hasAnyVd || hasAnyIk) ? 1 : 0;
                
                console.log(`МСУ1 ВД${number}: ${newValue ? 1 : 0}, конфиг:`, localFormData.msu1Config);
            } else {
                const newValue = !msu2Vd[vdKey];
                msu2Vd[vdKey] = newValue;
                
                // Обновляем конфиг
                const configKey = `vd${number}` as keyof TsMsuConfig;
                localFormData.msu2Config[configKey] = newValue ? 1 : 0;
                
                const hasAnyVd = msu2Vd.vd1 || msu2Vd.vd2 || msu2Vd.vd3;
                localFormData.msu2Config.prVdMsu = hasAnyVd ? 1 : 0;
                
                const hasAnyIk = Object.values(msu2Ik).some(v => v);
                localFormData.msu2Config.prMsu = (hasAnyVd || hasAnyIk) ? 1 : 0;
                
                console.log(`МСУ2 ВД${number}: ${newValue ? 1 : 0}, конфиг:`, localFormData.msu2Config);
            }
        } else {
            const ikKey = `ik${number}` as keyof typeof msu1Ik;
            
            if (msu === 'msu1') {
                const newValue = !msu1Ik[ikKey];
                msu1Ik[ikKey] = newValue;
                
                // Обновляем конфиг
                const configKey = `ik${number}` as keyof TsMsuConfig;
                localFormData.msu1Config[configKey] = newValue ? 1 : 0;
                
                const hasAnyIk = Object.values(msu1Ik).some(v => v);
                localFormData.msu1Config.prIkMsu = hasAnyIk ? 1 : 0;
                
                const hasAnyVd = msu1Vd.vd1 || msu1Vd.vd2 || msu1Vd.vd3;
                localFormData.msu1Config.prMsu = (hasAnyVd || hasAnyIk) ? 1 : 0;
                
                console.log(`МСУ1 ИК${number}: ${newValue ? 1 : 0}, конфиг:`, localFormData.msu1Config);
            } else {
                const newValue = !msu2Ik[ikKey];
                msu2Ik[ikKey] = newValue;
                
                // Обновляем конфиг
                const configKey = `ik${number}` as keyof TsMsuConfig;
                localFormData.msu2Config[configKey] = newValue ? 1 : 0;
                
                const hasAnyIk = Object.values(msu2Ik).some(v => v);
                localFormData.msu2Config.prIkMsu = hasAnyIk ? 1 : 0;
                
                const hasAnyVd = msu2Vd.vd1 || msu2Vd.vd2 || msu2Vd.vd3;
                localFormData.msu2Config.prMsu = (hasAnyVd || hasAnyIk) ? 1 : 0;
                
                console.log(`МСУ2 ИК${number}: ${newValue ? 1 : 0}, конфиг:`, localFormData.msu2Config);
            }
        }
    }

    function handleSubmit() {
        console.log('handleSubmit вызван');
        console.log('localFormData перед отправкой:', localFormData);
        console.log('msu1Config перед отправкой:', localFormData.msu1Config);
        console.log('msu2Config перед отправкой:', localFormData.msu2Config);
        console.log('kvdConfig перед отправкой:', localFormData.kvdConfig);
        
        if (!localFormData.startTime || localFormData.duration <= 0) {
            alert('Укажите время начала и длительность');
            return;
        }

        const dataToSubmit = { ...localFormData };
        
        // Создаем новые объекты для конфигов чтобы избежать ссылочных проблем
        dataToSubmit.msu1Config = { ...localFormData.msu1Config };
        dataToSubmit.msu2Config = { ...localFormData.msu2Config };
        if (localFormData.kvdConfig) {
            dataToSubmit.kvdConfig = { ...localFormData.kvdConfig };
        }
        
        console.log('Отправляемые данные:', dataToSubmit);
        
        if (isEditMode) {
            onUpdate?.(dataToSubmit);
        } else {
            onSubmit(dataToSubmit);
            resetForm();
        }
    }

    function resetForm() {
        resetCheckboxes();
        
        localFormData = {
            modeType: selectedMode, 
            ppiNum: 1,
            duration: 300,
            customerCode: 1,
            startTime: '10:00',
            // msu1Vd: [],
            // msu2Vd: [],
            msu1Config: getDefaultMsuConfig(),
            msu2Config: getDefaultMsuConfig(),
            kvdConfig: { // Важно инициализировать
                prMsu: 0,
                prBssd: 0,
                prZg: 0
            }
        };
        
        if (selectedMode) {
            const modeCode = MODE_ID_TO_CODE[selectedMode];
            if (modeCode && modeDurations[modeCode] !== undefined) {
                localFormData.duration = modeDurations[modeCode];
            }
        }
    }

    function resetCheckboxes() {
        msu1Vd = { vd1: false, vd2: false, vd3: false };
        msu2Vd = { vd1: false, vd2: false, vd3: false };
        msu1Ik = {
            ik4: false, ik5: false, ik6: false, ik7: false,
            ik8: false, ik9: false, ik10: false
        };
        msu2Ik = {
            ik4: false, ik5: false, ik6: false, ik7: false,
            ik8: false, ik9: false, ik10: false
        };
        
        // localFormData.msu1Vd = [];
        // localFormData.msu2Vd = [];
        localFormData.msu1Config = getDefaultMsuConfig();
        localFormData.msu2Config = getDefaultMsuConfig();
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
                        <option value={ppi.num}>{ppi.name}</option>
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
                        <select 
                            bind:value={localFormData.kvdConfig.prZg}
                        >
                            {#each zgOptions as zg}
                                <option value={zg.value}>{zg.label}</option>
                            {/each}
                        </select>
                    </div>
                </div>
            </div>
        {:else if selectedMode === 8}
            <!-- Режим TS - старая структура -->
            <div class="form-section">
                <span class="form-section_title">Комплект МСУ-ГС 1</span>
                <div class="ts-config-grid">
                    <div class="checkbox-group">
                        {#each [1, 2, 3] as vd}
                            <label class="checkbox-label">
                                <input 
                                    type="checkbox"
                                    checked={msu1Vd[`vd${vd}` as keyof typeof msu1Vd]}
                                    on:change={() => handleTsCheckbox('vd', 'msu1', vd)}
                                />
                                <span>ВД{vd}</span>
                            </label>
                        {/each}
                    </div>
                    
                    <div class="checkbox-group">
                        {#each [4, 5, 6, 7, 8, 9, 10] as ik}
                            <label class="checkbox-label">
                                <input 
                                    type="checkbox"
                                    checked={msu1Ik[`ik${ik}` as keyof typeof msu1Ik]}
                                    on:change={() => handleTsCheckbox('ik', 'msu1', ik)}
                                />
                                <span>ИК{ik}</span>
                            </label>
                        {/each}
                    </div>
                </div>
            </div>

            <div class="form-section">
                <span class="form-section_title">Комплект МСУ-ГС 2</span>
                <div class="ts-config-grid">
                    <div class="checkbox-group">
                        {#each [1, 2, 3] as vd}
                            <label class="checkbox-label">
                                <input 
                                    type="checkbox"
                                    checked={msu2Vd[`vd${vd}` as keyof typeof msu2Vd]}
                                    on:change={() => handleTsCheckbox('vd', 'msu2', vd)}
                                />
                                <span>ВД{vd}</span>
                            </label>
                        {/each}
                    </div>
                    
                    <div class="checkbox-group">
                        {#each [4, 5, 6, 7, 8, 9, 10] as ik}
                            <label class="checkbox-label">
                                <input 
                                    type="checkbox"
                                    checked={msu2Ik[`ik${ik}` as keyof typeof msu2Ik]}
                                    on:change={() => handleTsCheckbox('ik', 'msu2', ik)}
                                />
                                <span>ИК{ik}</span>
                            </label>
                        {/each}
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
                        step="300"
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
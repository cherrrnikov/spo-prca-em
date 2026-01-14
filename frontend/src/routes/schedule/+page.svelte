<script lang="ts">
    import CityLegend from "$lib/components/CityLegend.svelte";
    import FileMenu from "$lib/components/FileMenu.svelte";
    import ScheduleGrid from "$lib/components/ScheduleGrid.svelte";
    import type { UserResponse } from "$lib/types/auth";
    import type { TimeInterval, WorkMode } from "$lib/types/schedule";
    import { onMount } from "svelte";

    // Состояние приложения
    let isLoading = $state(true);
    let userData = $state<UserResponse | null>(null);
    let creationMode = $state<'operator' | 'assignment' | 'reference' | null>(null);
    
    // Параметры формы
    let scheduleStatus = $state<'main' | 'corrective'>('main');
    let selectedDate = $state('');
    let selectedTime = $state('');
    let shootingMode = $state<'default' | 'regular'>('default');
    let msuGsType = $state<'msu_gs_1' | 'msu_gs_2'>('msu_gs_1');
    
    // Данные с сервера (храним здесь временно)
    let serverData = $state<any>(null);
    let intervals = $state<TimeInterval[]>([
        {
            id: '1',
            mode: 'mode_1',
            startTime: '14:00',
            endTime: '15:30',
            city: 'moscow',
            color: '#4299e1',
            title: 'Интервал 1'
        },
        {
            id: '2',
            mode: 'mode_2',
            startTime: '09:00',
            endTime: '11:00',
            city: 'novosibirsk',
            color: '#48bb78',
            title: 'Интервал 2'
        },
        {
            id: '3',
            mode: 'mode_3',
            startTime: '16:00',
            endTime: '18:00',
            city: 'vladivostok',
            color: '#ed8936',
            title: 'Интервал 3'
        },
        {
            id: '4',
            mode: 'mode_1',
            startTime: '17:30',
            endTime: '17:45',
            city: 'novosibirsk',
            color: '#48bb78',
            title: 'Интервал 4'
        },
        {
            id: '5',
            mode: 'mode_2',
            startTime: '23:00',
            endTime: '23:59',
            city: 'novosibirsk',
            color: 'red',
            title: 'Интервал 5'
        }
    ]);

    const workModes: WorkMode[] = [
        {id: 'mode_1', label: 'Астрокорр.', order: '0'},
        {id: 'mode_2', label: 'Съемки', order: '1'},
        {id: 'mode_4', label: 'Распр. ОМИ', order: '3'},
        {id: 'mode_5', label: 'Режимы ТНП', order: '4'},
        {id: 'mode_6', label: 'Калибр. ВД', order: '5'},
        {id: 'mode_7', label: 'Техн. съемки', order: '6'},
        {id: 'mode_9', label: 'Юстировки ОНА', order: '8'}
    ];

    const cities = [
        {id: 'moscow', name: 'Москва', color: '#4299e1'},
        {id: 'novosibirsk', name: 'Новосибирск', color: '#48bb78'},
        {id: 'vladivostok', name: 'Владивосток', color: '#ed8936'},
        {id: 'moscow2', name: 'Москва2', color: '#4399e1'},
        {id: 'novosibirsk2', name: 'Новосибирск2', color: '#44bb78'},
        {id: 'vladivostok2', name: 'Владивосток2', color: '#ed8636'}
    ];

    onMount(() => {
        try {
            const userDataCookie = document.cookie
                .split('; ')
                .find(row => row.startsWith('user_data='));

            
            if (userDataCookie) {
                const userDataStr = userDataCookie.split('=')[1];
                const parsedData = JSON.parse(decodeURIComponent(userDataStr));
                
                userData = {
                    username: parsedData.username,
                    firstName: parsedData.firstName,
                    lastName: parsedData.lastName,
                    enabled: parsedData.enabled !== undefined ? parsedData.enabled : true,
                    accountLocked: parsedData.accountLocked !== undefined ? parsedData.accountLocked : false,
                    failedAttempts: parsedData.failedAttempts || 0,
                    lastLoginAt: parsedData.lastLoginAt,
                    lastLogoutAt: parsedData.lastLogoutAt || '',
                    roles: parsedData.roles || []
                };
                
                console.log('User data loaded:', userData);
            }
        } catch (error) {
            console.error('Error parsing user data:', error);
            userData = null;
        }

        const now = new Date();
        selectedDate = now.toISOString().split('T')[0];
        selectedTime = now.toTimeString().substring(0,5);

        setTimeout(() => {
            isLoading = false;
        }, 300);
    });

    function startOperatorCreation() {
        creationMode = 'operator';
        console.log('Начинаем создание ПРЦА по данным оператора')
    }

    function startAssignmentCreation() {
        creationMode = 'assignment';
        console.log('Начинаем создание ПРЦА по заданию на планирование');
    }

    function startReferenceCreation() {
        creationMode = 'reference';
        console.log('Начинаем создание ПРЦА по опорной ПРЦА');
    }

    function cancelCreation() {
        creationMode = null;
        scheduleStatus = 'main';
        shootingMode = 'default';
        msuGsType = 'msu_gs_1';
        serverData = null; 
    }

    async function loadOperatorData() {
        if (!selectedDate) {
            alert('Пожалуйста, выберите дату');
            return;
        }

        console.log('Начинаем загрузку данных...');

        try {
            const response = await fetch(`/api/schedule/proxy?date=${selectedDate}`);
            
            if (!response.ok) {
                if (response.status === 404) {
                    alert('Нет данных для выбранной даты');
                    return;
                }
                throw new Error(`Ошибка сервера: ${response.status}`);
            }
            
            const data = await response.json();
            serverData = data;
            
            console.log('Данные успешно получены:', data);
            alert(`Данные успешно загружены!`);
            
        } catch (error) {
            console.error('Ошибка при загрузке данных:', error);
            alert('Ошибка при загрузке данных');
        }
    }

    async function createSchedule() {
        console.log('Начинаем создание ПРЦА с параметрами:', {
            mode: creationMode,
            status: scheduleStatus,
            date: selectedDate,
            time: selectedTime,
            shootingMode,
            msuGsType
        });

        switch (creationMode) {
            case 'operator':
                await loadOperatorData();
                break;
                
            case 'assignment':
                alert('Создание по заданию на планирование (в разработке)');
                break;
                
            case 'reference':
                alert('Создание по опорной ПРЦА (в разработке)');
                break;
                
            default:
                alert('Выберите режим создания ПРЦА');
        }
    }

    function getFormTitle() {
        switch (creationMode) {
            case 'operator':
                return 'Создание ПРЦА по данным оператора';
            case 'assignment':
                return 'Создание ПРЦА по заданию на планирование';
            case 'reference':
                return 'Создание ПРЦА по опорной ПРЦА';
            default:
                return '';
        }
    }
</script>

<main class="schedule-page">
    <header class="schedule-header">
         {#if creationMode === 'operator'}
            <div class="creation-form-container">
                <div class="creation-form-header">
                    <div class="creation-title">{getFormTitle()}</div>
                </div>
                
                <div class="creation-form">
                    <div class="form-group">
                        <label class="form-label">Статус ПРЦА:</label>
                        <div class="radio-group">
                            <label class="radio-label">
                                <input 
                                    type="radio" 
                                    value="main"
                                    bind:group={scheduleStatus}
                                />
                                <span>Основная</span>
                            </label>
                            <label class="radio-label">
                                <input 
                                    type="radio" 
                                    value="corrective"
                                    bind:group={scheduleStatus}
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
                                bind:value={selectedDate}
                                class="date-input"
                            />
                            <input 
                                type="time" 
                                bind:value={selectedTime}
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
                                    value="default"
                                    bind:group={shootingMode}
                                />
                                <span>По умолчанию</span>
                            </label>
                            <label class="radio-label">
                                <input 
                                    type="radio" 
                                    value="no_shooting"
                                    bind:group={shootingMode}
                                />
                                <span>Без съемок</span>
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Признак МСУ ГС:</label>
                        <div class="radio-group">
                            <label class="radio-label">
                                <input 
                                    type="radio" 
                                    value="msu_gs_1"
                                    bind:group={msuGsType}
                                />
                                <span>Комплект МСУ ГС 1</span>
                            </label>
                            <label class="radio-label">
                                <input 
                                    type="radio" 
                                    value="msu_gs_2"
                                    bind:group={msuGsType}
                                />
                                <span>Комплект МСУ ГС 2</span>
                            </label>
                        </div>
                    </div>

                    <div class="form-actions">
                        <button 
                            on:click={createSchedule}
                            class="create-button"
                            disabled={isLoading}
                        >
                            {isLoading ? 'Загрузка...' : 'Загрузить данные'}
                        </button>
                        <button 
                            on:click={cancelCreation}
                            class="cancel-button"
                        >
                            Отмена
                        </button>
                    </div>
                </div>
            </div>
        {:else}
            <FileMenu 
                {userData}
                onOperatorCreate={startOperatorCreation}
                onAssignmentCreate={startAssignmentCreation}
                onReferenceCreate={startReferenceCreation}
            />
        {/if}
    </header>
    
    <div class="grid-container">
        {#if isLoading}
            <div class="loading">
                Загрузка...
            </div>
        {:else}
            <ScheduleGrid 
                {intervals}
                {workModes}
            />
        {/if}
    </div>

    <footer class="schedule-footer">
        <CityLegend {cities}/>
    </footer>
</main>

<style>
    .schedule-page {
        display: flex;
        flex-direction: column;
        height: 100vh;
        background: #f5f7fa;
        overflow: hidden;
    }

    .schedule-header {
        display: flex;
        align-items: center;
        padding: 0.5rem 1rem;
        background: white;
        border-bottom: 1px solid #e1e5e9;
        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
    }
    
    .schedule-header h1 {
        margin: 0;
        margin-left: 2rem;
        font-size: 1rem;
        color: #2d3748;
    }
    
    .grid-container {
        flex: 1;
        padding: 0;
        width: 100%;
        overflow: auto;
        display: flex;
        justify-content: center;
        align-items: flex-start;
    }

    .loading {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100%;
        color: #718096;
        font-size: 1.2rem;
    }

    .schedule-footer {
        display: flex;
        justify-content: end;
        padding: 0.5rem 2rem;
    }

    .creation-form-container {
        display: flex;
        flex-direction: column;
        width: 100%;
        gap: 0.75rem;
    }

    .creation-form-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    .creation-title {
        font-size: 1.1rem;
        font-weight: 600;
        color: #2d3748;
    }

    .creation-form {
        display: flex;
        align-items: center;
        gap: 2rem;
        flex-wrap: wrap;
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

    .radio-label input[type="radio"] {
        margin: 0;
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

    .date-input:hover,
    .time-input:hover {
        border-color: #a0aec0;
    }

    .date-input:focus,
    .time-input:focus {
        outline: none;
        border-color: #4299e1;
        box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1);
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

    /* Стили для блока с данными */
    .data-summary {
        display: flex;
        gap: 1.5rem;
        margin-left: auto;
        padding: 0.5rem 1rem;
        background: #f7fafc;
        border-radius: 6px;
        border: 1px solid #e2e8f0;
    }

    .summary-item {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
    }

    .summary-item .label {
        font-size: 0.75rem;
        color: #718096;
        font-weight: 500;
    }

    .summary-item .value {
        font-size: 0.875rem;
        color: #2d3748;
        font-weight: 600;
    }
</style>
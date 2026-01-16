<script lang="ts">
    import CityLegend from "$lib/components/CityLegend.svelte";
    import FileMenu from "$lib/components/FileMenu.svelte";
    import PpiSelectionModal from "$lib/components/PpiSelectionModal.svelte";
    import ScheduleGrid from "$lib/components/ScheduleGrid.svelte";
    import type { UserResponse } from "$lib/types/auth";
    import type {
    	CreateProgramRequest,
    	Id06KvdDto,
    	Id06TnpDto,
    	Id06TsDto,
    	OperatorData,
    	Ppi,
    	PpiAssignment,
    	PpiSelectionModal as PpiModalType,
    	ProgramCreationState,
    	ProgramModeData,
    	TimeInterval,
    	WorkMode
    } from "$lib/types/schedule";
    import { onMount } from "svelte";

    // Состояние приложения
    let isLoading = $state(false);
    let userData = $state<UserResponse | null>(null);
    let creationMode = $state<'operator' | 'reference' | null>(null);
    
    // Параметры формы
    let scheduleStatus = $state<'main' | 'corrective'>('main');
    let selectedDate = $state('');
    let selectedTime = $state('');
    let shootingMode = $state<'default' | 'no_shooting'>('default');
    let msuGsType = $state<'msu_gs_1' | 'msu_gs_2'>('msu_gs_1');
    
    // Данные с сервера
    let operatorData = $state<OperatorData | null>(null);
    
    // Интервалы для отображения в сетке
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

    // Данные для модального окна ППИ
    let ppiModal = $state<PpiModalType>({
        isOpen: false,
        currentRecord: null,
        recordType: null,
        recordIndex: 0,
        totalRecords: 0,
        selectedPpi: null,
        recordTitle: ''
    });

    // Состояние процесса создания
    let creationState = $state<ProgramCreationState>({
        step: 'form',
        currentRecordType: null,
        currentRecordIndex: 0,
        processedRecords: 0,
        totalRecords: 0
    });

    let ppiAssignments = $state<PpiAssignment[]>([]);

    let programData = $state<CreateProgramRequest | null>(null);

    const ppiList: Ppi[] = [
        { id: 1, name: '0 - Обнинск', numPpi: 1 },
        { id: 2, name: '1 - Долгопрудный', numPpi: 2 },
        { id: 3, name: '2 - Новосибирск', numPpi: 3 },
        { id: 4, name: '3 - Хабаровск', numPpi: 4 },
        { id: 5, name: '4 - Байконур', numPpi: 5 },
        { id: 6, name: '5 - Ханты-Мансийск', numPpi: 6 },
        { id: 7, name: '6 - Железногорск', numPpi: 7 },
        { id: 8, name: '7 - Улан-Удэ', numPpi: 8 },
        { id: 9, name: '8 - Москва (НЦ ОМЗ)', numPpi: 9 },
        { id: 10, name: '9 - Москва (НИЦ "Планета")', numPpi: 10 }
    ];

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
    });

    function startOperatorCreation() {
        creationMode = 'operator';
        creationState.step = 'form';
        console.log('Начинаем создание ПРЦА по данным оператора')
    }

    function startReferenceCreation() {
        creationMode = 'reference';
        console.log('Начинаем создание ПРЦА по опорной ПРЦА');
        alert('Создание по опорной ПРЦА (в разработке)');
    }

    function cancelCreation() {
        creationMode = null;
        scheduleStatus = 'main';
        shootingMode = 'default';
        msuGsType = 'msu_gs_1';
        operatorData = null;
        ppiAssignments = [];
        creationState.step = 'form';
        ppiModal.isOpen = false;
    }

    async function loadOperatorData() {
        if (!selectedDate) {
            alert('Пожалуйста, выберите дату');
            return;
        }

        console.log('Загрузка данных оператора для даты:', selectedDate);
        isLoading = true;

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
            operatorData = data;
            
            console.log('Данные оператора получены:', data);
            
            creationState.step = 'ppi_selection';
            
            const totalRecords = 
                (data.kvdList?.length || 0) + 
                (data.tnpList?.length || 0) + 
                (data.tsList?.length || 0);
            
            creationState.totalRecords = totalRecords;
            
            if (data.kvdList && data.kvdList.length > 0) {
                await processKvdRecords(data.kvdList);
            }
            
            if (data.tnpList && data.tnpList.length > 0) {
                await processTnpRecords(data.tnpList);
            }
            
            if (data.tsList && data.tsList.length > 0) {
                await processTsRecords(data.tsList);
            }
            
            await prepareProgramData();
            
        } catch (error) {
            console.error('Ошибка при загрузке данных:', error);
            alert('Ошибка при загрузке данных: ' + (error as Error).message);
        } finally {
            isLoading = false;
        }
    }

    async function processKvdRecords(kvdList: Id06KvdDto[]) {
        creationState.currentRecordType = 'kvd';
        
        for (let i = 0; i < kvdList.length; i++) {
            const record = kvdList[i];
            
            const existingAssignment = ppiAssignments.find(a => 
                a.recordId === record.id && a.recordType === 'kvd'
            );
            
            if (existingAssignment) {
                console.log(`ППИ уже выбран для KVD записи ${record.id}`);
                creationState.processedRecords++;
                continue;
            }
            
            await showPpiModal(record, 'kvd', i, kvdList.length, 'Калибровка ВД');
        }
    }

    async function processTnpRecords(tnpList: Id06TnpDto[]) {
        creationState.currentRecordType = 'tnp';
        
        for (let i = 0; i < tnpList.length; i++) {
            const record = tnpList[i];
            
            const existingAssignment = ppiAssignments.find(a => 
                a.recordId === record.id && a.recordType === 'tnp'
            );
            
            if (existingAssignment) {
                console.log(`ППИ уже выбран для TNP записи ${record.id}`);
                creationState.processedRecords++;
                continue;
            }
            
            await showPpiModal(record, 'tnp', i, tnpList.length, 'Режим ТНП');
        }
    }

    async function processTsRecords(tsList: Id06TsDto[]) {
        creationState.currentRecordType = 'ts';
        
        for (let i = 0; i < tsList.length; i++) {
            const record = tsList[i];
            
            const existingAssignment = ppiAssignments.find(a => 
                a.recordId === record.id && a.recordType === 'ts'
            );
            
            if (existingAssignment) {
                console.log(`ППИ уже выбран для TS записи ${record.id}`);
                creationState.processedRecords++;
                continue;
            }
            
            await showPpiModal(record, 'ts', i, tsList.length, 'Технологическая съемка');
        }
    }

    async function showPpiModal(
        record: Id06KvdDto | Id06TnpDto | Id06TsDto,
        recordType: 'kvd' | 'tnp' | 'ts',
        index: number,
        total: number,
        title: string
    ) {
        return new Promise<void>((resolve) => {
            ppiModal = {
                isOpen: true,
                currentRecord: record,
                recordType,
                recordIndex: index,
                totalRecords: total,
                selectedPpi: null,
                recordTitle: title
            };
            
            const checkModalClosed = () => {
                if (!ppiModal.isOpen) {
                    resolve();
                } else {
                    setTimeout(checkModalClosed, 100);
                }
            };
            checkModalClosed();
        });
    }

    function handleApplyPpi(
        record: Id06KvdDto | Id06TnpDto | Id06TsDto, 
        ppi: Ppi, 
        applyToAll: boolean
    ) {
        if (applyToAll && ppiModal.recordType && operatorData) {
            let records: (Id06KvdDto | Id06TnpDto | Id06TsDto)[] = [];
            
            switch (ppiModal.recordType) {
                case 'kvd':
                    records = operatorData.kvdList || [];
                    break;
                case 'tnp':
                    records = operatorData.tnpList || [];
                    break;
                case 'ts':
                    records = operatorData.tsList || [];
                    break;
            }
            
            records.forEach(rec => {
                addPpiAssignment(rec.id, ppiModal.recordType!, ppi);
            });
            
            console.log(`Применен ППИ ${ppi.numPpi} для всех ${records.length} записей типа ${ppiModal.recordType}`);
        } else {
            addPpiAssignment(record.id, ppiModal.recordType!, ppi);
            console.log(`Применен ППИ ${ppi.numPpi} для записи ${record.id} типа ${ppiModal.recordType}`);
        }
        
        creationState.processedRecords++;
        ppiModal.isOpen = false;
    }

    function addPpiAssignment(recordId: number, recordType: 'kvd' | 'tnp' | 'ts', ppi: Ppi) {
        ppiAssignments = ppiAssignments.filter(a => 
            !(a.recordId === recordId && a.recordType === recordType)
        );
        
        ppiAssignments.push({
            recordId,
            recordType,
            ppiId: ppi.id,
            ppiNum: ppi.numPpi
        });
    }

    function closePpiModal() {
        creationState.processedRecords++;
        ppiModal.isOpen = false;
    }

    async function prepareProgramData() {
        if (!operatorData) return;
        
        console.log('Подготовка данных для сохранения ПРЦА...');
        creationState.step = 'review';
        
        const mainData = {
            numRp: generateProgramNumber(),
            numKa: operatorData.main.nKa,
            dateOn: `${selectedDate}T${selectedTime}:00`,
            dateOff: calculateDateOff(),
            typeRp: scheduleStatus === 'main' ? 3 : 5,
            prOtpr: 0
        };
        
        const modes: ProgramModeData[] = [];
        
        if (operatorData.kvdList) {
            for (const kvd of operatorData.kvdList) {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === kvd.id && a.recordType === 'kvd'
                );
                
                if (assignment) {
                    const mode: ProgramModeData = {
                        numRp: mainData.numRp,
                        numKa: mainData.numKa,
                        dateOn: kvd.dn,
                        dateOff: kvd.dk,
                        kodMode: 3,
                        numPpi: assignment.ppiNum,
                        dlit: calculateDuration(kvd.dn, kvd.dk),
                        kvdData: {
                            prMsu: kvd.prMsu,
                            prBssd: kvd.prBssd,
                            prZg: kvd.prZg
                        }
                    };
                    modes.push(mode);
                }
            }
        }
        
        if (operatorData.tnpList) {
            for (const tnp of operatorData.tnpList) {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === tnp.id && a.recordType === 'tnp'
                );
                
                if (assignment) {
                    const mode: ProgramModeData = {
                        numRp: mainData.numRp,
                        numKa: mainData.numKa,
                        dateOn: tnp.dn,
                        dateOff: tnp.dk,
                        kodMode: 4,
                        numPpi: assignment.ppiNum,
                        dlit: tnp.dlit || calculateDuration(tnp.dn, tnp.dk),
                        tnpData: {}
                    };
                    modes.push(mode);
                }
            }
        }
        
        if (operatorData.tsList) {
            for (const ts of operatorData.tsList) {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === ts.id && a.recordType === 'ts'
                );
                
                if (assignment) {
                    const mode: ProgramModeData = {
                        numRp: mainData.numRp,
                        numKa: mainData.numKa,
                        dateOn: ts.dn,
                        dateOff: ts.dk,
                        kodMode: 5,
                        numPpi: assignment.ppiNum,
                        dlit: calculateDuration(ts.dn, ts.dk),
                        tsData: {
                            tip: ts.tip,
                            reg: ts.reg,
                            dlit: calculateDuration(ts.dn, ts.dk),
                            prMsu1: ts.prMsu1,
                            prVdMsu1: ts.prVdMsu1,
                            prIkMsu1: ts.prIkMsu1,
                            prVd1_1: ts.prVd1_1,
                            prVd2_1: ts.prVd2_1,
                            prVd3_1: ts.prVd3_1,
                            prIk4_1: ts.prIk4_1,
                            prIk5_1: ts.prIk5_1,
                            prIk6_1: ts.prIk6_1,
                            prIk7_1: ts.prIk7_1,
                            prIk8_1: ts.prIk8_1,
                            prIk9_1: ts.prIk9_1,
                            prIk10_1: ts.prIk10_1,
                            prMsu2: ts.prMsu2,
                            prVdMsu2: ts.prVdMsu2,
                            prIkMsu2: ts.prIkMsu2,
                            prVd1_2: ts.prVd1_2,
                            prVd2_2: ts.prVd2_2,
                            prVd3_2: ts.prVd3_2,
                            prIk4_2: ts.prIk4_2,
                            prIk5_2: ts.prIk5_2,
                            prIk6_2: ts.prIk6_2,
                            prIk7_2: ts.prIk7_2,
                            prIk8_2: ts.prIk8_2,
                            prIk9_2: ts.prIk9_2,
                            prIk10_2: ts.prIk10_2,
                            prOtklZg: ts.prOtklZg
                        }
                    };
                    modes.push(mode);
                }
            }
        }
        
        programData = {
            mainData,
            modes
        };
        
        console.log('Данные ПРЦА подготовлены:', programData);
        
        alert(`Данные подготовлены для сохранения! Всего режимов: ${modes.length}`);
    }

    function generateProgramNumber(): number {
        return Math.floor(Date.now() / 1000);
    }

    function calculateDateOff(): string {
        if (!operatorData) return `${selectedDate}T23:59:59`;
        
        let latestDate = new Date(`${selectedDate}T${selectedTime}:00`);
        
        const allRecords = [
            ...(operatorData.kvdList || []),
            ...(operatorData.tnpList || []),
            ...(operatorData.tsList || [])
        ];
        
        allRecords.forEach(record => {
            const endDate = new Date(record.dk);
            if (endDate > latestDate) {
                latestDate = endDate;
            }
        });
        
        return latestDate.toISOString();
    }

    function calculateDuration(startStr: string, endStr: string): number {
        const start = new Date(startStr);
        const end = new Date(endStr);
        return Math.floor((end.getTime() - start.getTime()) / 1000);
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
        }
    }

    async function saveProgram() {
        if (!programData) {
            alert('Нет данных для сохранения');
            return;
        }

        creationState.step = 'saving';
        isLoading = true;

        try {
            const response = await fetch('/api/programs/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(programData)
            });

            if (!response.ok) {
                throw new Error(`Ошибка сохранения: ${response.status}`);
            }

            const result = await response.json();
            console.log('ПРЦА успешно сохранена:', result);
            alert('ПРЦА успешно сохранена!');
            
            cancelCreation();
            
        } catch (error) {
            console.error('Ошибка при сохранении ПРЦА:', error);
            alert('Ошибка при сохранении ПРЦА: ' + (error as Error).message);
        } finally {
            isLoading = false;
        }
    }

    function getFormTitle() {
        switch (creationMode) {
            case 'operator':
                return 'Создание ПРЦА по данным оператора';
            case 'reference':
                return 'Создание ПРЦА по опорной ПРЦА';
            default:
                return '';
        }
    }

    function getCreationStatusText() {
        switch (creationState.step) {
            case 'form':
                return 'Заполните параметры создания ПРЦА';
            case 'ppi_selection':
                return `Выбор ППИ: ${creationState.processedRecords} из ${creationState.totalRecords} записей обработано`;
            case 'review':
                return 'Данные подготовлены. Проверьте и сохраните ПРЦА';
            case 'saving':
                return 'Сохранение ПРЦА...';
            default:
                return '';
        }
    }

    function getStatistics() {
        if (!operatorData) return null;
        
        const kvdCount = operatorData.kvdList?.length || 0;
        const tnpCount = operatorData.tnpList?.length || 0;
        const tsCount = operatorData.tsList?.length || 0;
        
        const kvdWithPpi = ppiAssignments.filter(a => a.recordType === 'kvd').length;
        const tnpWithPpi = ppiAssignments.filter(a => a.recordType === 'tnp').length;
        const tsWithPpi = ppiAssignments.filter(a => a.recordType === 'ts').length;
        
        return {
            total: kvdCount + tnpCount + tsCount,
            kvd: { total: kvdCount, withPpi: kvdWithPpi },
            tnp: { total: tnpCount, withPpi: tnpWithPpi },
            ts: { total: tsCount, withPpi: tsWithPpi }
        };
    }

    const stats = getStatistics();
</script>

<main class="schedule-page">
    <header class="schedule-header">
        {#if creationMode === 'operator'}
            <div class="creation-form-container">
                <div class="creation-form-header">
                    <div class="creation-title">{getFormTitle()}</div>
                    
                    {#if creationState.step !== 'form'}
                        <div class="creation-status">
                            {getCreationStatusText()}
                        </div>
                    {/if}
                </div>
                
                {#if creationState.step === 'form'}
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
                
                {:else if creationState.step === 'review' && stats}
                    <div class="review-section">
                        <div class="review-stats">
                            <h3>Статистика выбранных ППИ:</h3>
                            <div class="stats-grid">
                                <div class="stat-item">
                                    <span class="stat-label">Калибровка ВД:</span>
                                    <span class="stat-value">{stats.kvd.withPpi}/{stats.kvd.total}</span>
                                </div>
                                <div class="stat-item">
                                    <span class="stat-label">Режимы ТНП:</span>
                                    <span class="stat-value">{stats.tnp.withPpi}/{stats.tnp.total}</span>
                                </div>
                                <div class="stat-item">
                                    <span class="stat-label">Техн. съемки:</span>
                                    <span class="stat-value">{stats.ts.withPpi}/{stats.ts.total}</span>
                                </div>
                                <div class="stat-item total">
                                    <span class="stat-label">Всего:</span>
                                    <span class="stat-value">{ppiAssignments.length}/{stats.total}</span>
                                </div>
                            </div>
                        </div>
                        
                        <div class="review-actions">
                            <button 
                                on:click={saveProgram}
                                class="save-button"
                                disabled={isLoading || ppiAssignments.length === 0}
                            >
                                {isLoading ? 'Сохранение...' : 'Сохранить ПРЦА'}
                            </button>
                            <button 
                                on:click={cancelCreation}
                                class="cancel-button"
                            >
                                Отмена
                            </button>
                        </div>
                    </div>
                {/if}
            </div>
        
        {:else}
            <FileMenu 
                {userData}
                onOperatorCreate={startOperatorCreation}
                onReferenceCreate={startReferenceCreation}
            />
        {/if}
    </header>
    
    <div class="grid-container">
        {#if isLoading && creationState.step !== 'saving'}
            <div class="loading">
                <div class="loading-spinner"></div>
                <div class="loading-text">
                    {#if creationState.step === 'ppi_selection'}
                        Обработка записей: {creationState.processedRecords} из {creationState.totalRecords}
                    {:else}
                        Загрузка...
                    {/if}
                </div>
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

    <PpiSelectionModal
        {ppiList}
        modalData={ppiModal}
        closeModal={closePpiModal}
        applyPpi={handleApplyPpi}
    />
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
        width: 100%;
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

    .creation-status {
        background: #4299e1;
        color: white;
        padding: 0.5rem 1rem;
        border-radius: 4px;
        font-size: 0.9rem;
        font-weight: 500;
        margin-left: auto;
    }

    .review-section {
        width: 100%;
        padding: 1rem 0;
    }

    .review-stats {
        background: #f7fafc;
        border-radius: 8px;
        padding: 1.5rem;
        margin-bottom: 1.5rem;
        border: 1px solid #e2e8f0;
    }

    .review-stats h3 {
        margin: 0 0 1rem 0;
        color: #2d3748;
        font-size: 1.1rem;
        font-weight: 600;
    }

    .stats-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 1rem;
    }

    @media (max-width: 768px) {
        .stats-grid {
            grid-template-columns: 1fr;
        }
    }

    .stat-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem;
        background: white;
        border-radius: 6px;
        border: 1px solid #e2e8f0;
    }

    .stat-item.total {
        background: #ebf8ff;
        border-color: #4299e1;
        font-weight: 600;
    }

    .stat-label {
        color: #4a5568;
        font-size: 0.9rem;
    }

    .stat-value {
        color: #2d3748;
        font-weight: 600;
        font-size: 1rem;
    }

    .review-actions {
        display: flex;
        justify-content: flex-end;
        gap: 1rem;
    }

    .save-button {
        background: linear-gradient(135deg, #48bb78, #38a169);
        color: white;
        border: none;
        padding: 0.75rem 2rem;
        border-radius: 6px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
    }

    .save-button:hover:not(:disabled) {
        background: linear-gradient(135deg, #38a169, #2f855a);
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(72, 187, 120, 0.3);
    }

    .save-button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    .loading {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        height: 100%;
        gap: 1rem;
    }

    .loading-spinner {
        width: 40px;
        height: 40px;
        border: 3px solid #e2e8f0;
        border-top: 3px solid #4299e1;
        border-radius: 50%;
        animation: spin 1s linear infinite;
    }

    .loading-text {
        color: #4a5568;
        font-size: 1rem;
    }

    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }
</style>
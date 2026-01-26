<script lang="ts">
    import { onMount } from 'svelte';
    
    import CityLegend from '$lib/components/CityLegend.svelte';
    import FileMenu from '$lib/components/FileMenu.svelte';
    import ScheduleGrid from '$lib/components/ScheduleGrid.svelte';
    import type { UserResponse } from '$lib/types/auth';
    import type {
    	CreatedProgramData,
    	ModeCreationForm,
    	OperatorData,
    	PpiAssignment,
    	ProgramModeData,
    	TimeInterval,
    	WorkMode
    } from '$lib/types/schedule';
    import CreationHeader from '../../features/schedule-creation/components/CreationHeader.svelte';
    import ModeCreationFormComponent from '../../features/schedule-creation/components/ModeCreationForm.svelte';
    import { ScheduleCreationService } from '../../features/services/scheduleCreation.service';

    const cities = [
        { id: 'obninsk', name: 'Обнинск', color: '#f4fc0a' },
        { id: 'dolgoprudniy', name: 'Долгопрудный', color: '#b80afc' },
        { id: 'novosibirsk', name: 'Новосибирск', color: '#0afcf4' },
        { id: 'khabarovsk', name: 'Хабаровск', color: '#593315' },
        { id: 'baykonur', name: 'Байконур', color: '#152359' },
        { id: 'khanty-mansiysk', name: 'Ханты-Мансийск', color: '#78866b' },
        { id: 'zheleznogorsk', name: 'Железногорск', color: '#6110b3' },
        { id: 'ulan-ude', name: 'Улан-Удэ', color: '#6197c9' },
        { id: 'moscow-omz', name: 'Москва (НЦ ОМЗ)', color: '#1a5216' },
        { id: 'moscow-planeta', name: 'Москва (ФГБУ НИЦ "Планета")', color: '#24f016' }
    ];

    
    const workModes: WorkMode[] = [
        { id: 9, label: 'Астрокорр.', order: '0' },
        { id: 1, label: 'Съемки', order: '1' },
        { id: 2, label: 'Распр. ОМИ', order: '2' },
        { id: 4, label: 'Режимы ТНП', order: '3' },
        { id: 7, label: 'Калибр. ВД', order: '4' },
        { id: 8, label: 'Техн. съемки', order: '5' },
        { id: 6, label: 'Юстировки ОНА', order: '6' }
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
    
    export const customerCodes = [
        { value: 1, label: '01 - Заказчик 1'},
        { value: 2, label: '02 - Заказчик 2'},
        { value: 3, label: '03 - Заказчик 3'},
        { value: 4, label: '04 - Заказчик 4'},
        { value: 5, label: '05 - Заказчик 5'}
    ];

    let userData = $state<UserResponse | null>(null);
    let creationMode = $state<'operator' | 'reference' | null>(null);
    let intervals = $state<TimeInterval[]>([]);
    let operatorData = $state<OperatorData | null>(null);
    let ppiAssignments = $state<PpiAssignment[]>([]);
    let operatorDataLoaded = $state(false); 

    let selectedMode = $state<number | null>(null);
    let createdPrograms = $state<CreatedProgramData[]>([]);
    let currentFormData = $state<ModeCreationForm>({
        modeType: null,
        ppiNum: 1,
        duration: 300,
        customerCode: 5,
        startTime: '10:00',
        msu1Vd: [],
        msu2Vd: [],
        msu1Config: {
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
        },
        msu2Config: {
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
        }
    });
    
    onMount(() => {
        loadUserData();
    });
    
    function loadUserData() {
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
    }

    function checkIntervalOverlap(
        newStartTime: string,
        newDuration: number,
        modeId: number
    ): { overlaps: boolean; conflictingInterval?: TimeInterval } {
        const newEndTime = calculateEndTime(newStartTime, newDuration);
        const newStartMinutes = timeToMinutes(newStartTime);
        const newEndMinutes = timeToMinutes(newEndTime);
        
        const allIntervals = [
            ...intervals,
            ...(operatorData ? 
                ScheduleCreationService.convertToTimeIntervals(operatorData, ppiAssignments, workModes) : 
                [])
        ];
        
        for (const interval of allIntervals) {
            if (interval.mode !== modeId) {
                continue;
            }
            
            const existingStartMinutes = timeToMinutes(interval.startTime);
            const existingEndMinutes = timeToMinutes(interval.endTime);
            
            const overlaps = (
                (newStartMinutes >= existingStartMinutes && newStartMinutes < existingEndMinutes) ||
                (newEndMinutes > existingStartMinutes && newEndMinutes <= existingEndMinutes) ||
                (newStartMinutes <= existingStartMinutes && newEndMinutes >= existingEndMinutes)
            );
            
            if (overlaps) {
                return { 
                    overlaps: true, 
                    conflictingInterval: interval 
                };
            }
        }
        
        return { overlaps: false };
    }
    
    function timeToMinutes(time: string): number {
        const [hours, minutes] = time.split(':').map(Number);
        return hours * 60 + minutes;
    }
    
    function findAvailableTimeSlot(
        duration: number,
        modeId: number
    ): { startTime: string; available: boolean } | null {
        const dayStart = 0; // 00:00
        const dayEnd = 24 * 60; // 24:00

        const allIntervals = [
        ...intervals,
        ...(operatorData ? 
            ScheduleCreationService.convertToTimeIntervals(operatorData, ppiAssignments, workModes) : 
            [])
    ];
        
        const modeIntervals = allIntervals.filter(interval => interval.mode === modeId);
        
        if (modeIntervals.length === 0) {
            return {
                startTime: '00:00',
                available: true
            };
        }
        
        const sortedIntervals = [...modeIntervals].sort((a, b) => 
            timeToMinutes(a.startTime) - timeToMinutes(b.startTime)
        );
        
        const firstIntervalStart = timeToMinutes(sortedIntervals[0].startTime);
        if (firstIntervalStart >= duration) {
            return {
                startTime: minutesToTime(0),
                available: true
            };
        }
        
        // Проверяем окна между интервалами
        for (let i = 0; i < sortedIntervals.length - 1; i++) {
            const currentEnd = timeToMinutes(sortedIntervals[i].endTime);
            const nextStart = timeToMinutes(sortedIntervals[i + 1].startTime);
            const gap = nextStart - currentEnd;
            
            if (gap >= duration) {
                return {
                    startTime: sortedIntervals[i].endTime,
                    available: true
                };
            }
        }
        
        // Проверяем окно после последнего интервала
        const lastIntervalEnd = timeToMinutes(sortedIntervals[sortedIntervals.length - 1].endTime);
        if (dayEnd - lastIntervalEnd >= duration) {
            return {
                startTime: sortedIntervals[sortedIntervals.length - 1].endTime,
                available: true
            };
        }
        
        return null;
    }
    
    function minutesToTime(minutes: number): string {
        const hours = Math.floor(minutes / 60);
        const mins = minutes % 60;
        return `${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}`;
    }
    
    function startOperatorCreation() {
        creationMode = 'operator';
    }
    
    function startReferenceCreation() {
        creationMode = 'reference';
        alert('Создание по опорной ПРЦА (в разработке)');
    }
    
    function handleCreationCancel() {
        creationMode = null;
    }
    
    function updateIntervalsFromOperatorData(
        newOperatorData: OperatorData,
        newPpiAssignments: PpiAssignment[]
    ) {
        operatorData = newOperatorData;
        ppiAssignments = newPpiAssignments;

        creationMode = 'operator';
        operatorDataLoaded = true;

        const newIntervals = ScheduleCreationService.convertToTimeIntervals(
            newOperatorData,
            newPpiAssignments,
            workModes
        );
        
        intervals = newIntervals;
    }

    function handleModeSelect(modeId: number) {
        if (!operatorDataLoaded) {
            console.log("Данные оператора не загружены");
            return;
        }

        if (creationMode !== 'operator') {
            return;
        }

        // let formModeType: 'kvd' | 'tnp' | 'ts' | 's' | 'omi' | 'ona' | 'astr' | null = null;
        // let defaultDuration = 300;
        
        // switch(modeId) {
        //     case 'mode_1': formModeType = 'astr'; defaultDuration = 300; break;
        //     case 'mode_2': formModeType = 's'; defaultDuration = 420; break;
        //     case 'mode_3': formModeType = 'omi'; defaultDuration = 60; break;
        //     case 'mode_4': formModeType = 'tnp'; defaultDuration = 516; break;
        //     case 'mode_5': formModeType = 'kvd'; defaultDuration = 420; break;
        //     case 'mode_6': formModeType = 'ts'; defaultDuration = 420; break;
        //     case 'mode_7': formModeType = 'ona'; defaultDuration = 60; break;
        //     default:
        //         console.warn('Unknown mode selected:', modeId);
        //         return;
        // }
        
        selectedMode = modeId;
        
        currentFormData.modeType = modeId;
        currentFormData.duration = 300;
        currentFormData.ppiNum = 1;
        currentFormData.customerCode = 5;
        currentFormData.startTime = '10:00';
        currentFormData.msu1Vd = [];
        currentFormData.msu2Vd = [];
        
        currentFormData.msu1Config = {
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
        currentFormData.msu2Config = {
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

    function handleModeFormSubmit(formData: ModeCreationForm) {
        if (!operatorDataLoaded) {
            console.log("Данные оператора не загружены");
            return;
        }

        if (creationMode !== 'operator') {
            return;
        }

        const modeId = formData.modeType!;
        const overlapCheck = checkIntervalOverlap(
            formData.startTime, 
            formData.duration, 
            modeId
        );
        
        if (overlapCheck.overlaps) {
            const conflicting = overlapCheck.conflictingInterval;
            alert(`Ошибка: интервал пересекается с существующим интервалом\n` +
                  `Время конфликта: ${conflicting?.startTime} - ${conflicting?.endTime}\n` +
                  `Попробуйте выбрать другое время или уменьшить длительность.`);
            return;
        }

        const tempId = `created_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        
        const modeData = createProgramModeData(formData, tempId);
        console.log('Создан новый интервал с данными:', modeData);
        const timeInterval = createTimeInterval(formData, tempId);
        
        createdPrograms = [...createdPrograms, {
            tempId,
            modeData,
            timeInterval
        }];
        
        intervals = [...intervals, timeInterval];
        
        selectedMode = null;
    }
    
   function createProgramModeData(formData: ModeCreationForm, tempId: string): ProgramModeData {

    const mainId = operatorData?.main.id || 0;

        const baseData = {
            numRp: 0,
            numKa: operatorData?.main.nKa || 1,
            dateOn: calculateDateFromTime(formData.startTime),
            dateOff: calculateEndDate(formData.startTime, formData.duration),
            kodMode: formData.modeType!,
            numPpi: formData.ppiNum,
            dlit: formData.duration,
            zakazchik: getCustomerLabel(formData.customerCode)
        };
        
        if (formData.modeType === 7) {
            return {
                ...baseData,
                kvdData: {
                    id: 0,
                    idMain: mainId,
                    dn: baseData.dateOn,
                    dk: baseData.dateOff,
                    prMsu: formData.msu1Vd.length > 0 ? 1 : 0,
                    prBssd: formData.msu2Vd.length > 0 ? 1 : 0,
                    prZg: 0
                }
            };
        } else if (formData.modeType === 8) {
            return {
                ...baseData,
                tsData: {
                    id: 0,
                    idMain: mainId,
                    dn: baseData.dateOn,
                    dk: baseData.dateOff,
                    tip: 1,
                    reg: 1,
                    prMsu1: formData.msu1Config.prMsu,
                    prVdMsu1: formData.msu1Config.prVdMsu,
                    prIkMsu1: formData.msu1Config.prIkMsu,
                    prVd1_1: formData.msu1Config.vd1,
                    prVd2_1: formData.msu1Config.vd2,
                    prVd3_1: formData.msu1Config.vd3,
                    prIk4_1: formData.msu1Config.ik4,
                    prIk5_1: formData.msu1Config.ik5,
                    prIk6_1: formData.msu1Config.ik6,
                    prIk7_1: formData.msu1Config.ik7,
                    prIk8_1: formData.msu1Config.ik8,
                    prIk9_1: formData.msu1Config.ik9,
                    prIk10_1: formData.msu1Config.ik10,
                    prMsu2: formData.msu2Config.prMsu,
                    prVdMsu2: formData.msu2Config.prVdMsu,
                    prIkMsu2: formData.msu2Config.prIkMsu,
                    prVd1_2: formData.msu2Config.vd1,
                    prVd2_2: formData.msu2Config.vd2,
                    prVd3_2: formData.msu2Config.vd3,
                    prIk4_2: formData.msu2Config.ik4,
                    prIk5_2: formData.msu2Config.ik5,
                    prIk6_2: formData.msu2Config.ik6,
                    prIk7_2: formData.msu2Config.ik7,
                    prIk8_2: formData.msu2Config.ik8,
                    prIk9_2: formData.msu2Config.ik9,
                    prIk10_2: formData.msu2Config.ik10,
                    prOtklZg: 0
                }
            };
        } else if (formData.modeType === 4) {
            return {
                ...baseData,
                tnpData: {
                    id: 0,
                    idMain: mainId,
                    dn: baseData.dateOn,
                    dk: baseData.dateOff,
                    dlit: formData.duration
                }
            };
        } else {
            return baseData;
        }
    }
    
    function createTimeInterval(formData: ModeCreationForm, tempId: string): TimeInterval {
        const endTime = calculateEndTime(formData.startTime, formData.duration);
        
        const interval = {
            id: tempId,
            mode: formData.modeType!,
            startTime: formData.startTime,
            endTime,
            city: ScheduleCreationService.getCityByPpi(formData.ppiNum),
            color: ScheduleCreationService.getColorByPpi(formData.ppiNum),
            title: getModeTitle(formData.modeType!),
            ppi: formData.ppiNum, 
            dlit: formData.duration
        };
        
        return interval;
    }
    
    function calculateDateFromTime(timeString: string): string {
        const today = new Date();
        const [hours, minutes] = timeString.split(':').map(Number);
        today.setHours(hours, minutes, 0, 0);
        return today.toISOString();
    }
    
    function calculateEndDate(startTime: string, duration: number): string {
        const startDate = new Date(calculateDateFromTime(startTime));
        const endDate = new Date(startDate.getTime() + duration * 1000);
        return endDate.toISOString();
    }
    
    function calculateEndTime(startTime: string, duration: number): string {
        const [hours, minutes] = startTime.split(':').map(Number);
        const totalMinutes = hours * 60 + minutes + Math.floor(duration / 60);
        const endHours = Math.floor(totalMinutes / 60) % 24;
        const endMinutes = totalMinutes % 60;
        return `${endHours.toString().padStart(2, '0')}:${endMinutes.toString().padStart(2, '0')}`;
    }
    
    function getModeTitle(modeType: number): string {
        switch(modeType) {
            case 9: return 'Астрокоррекции';
            case 1: return 'Съемки';
            case 2: return 'Распр. ОМИ';
            case 4: return 'Режим ТНП';
            case 7: return 'Калибровка ВД';
            case 8: return 'Технологическая съемка';
            case 6: return 'Юстировки ОНА';
            default: return 'Режим';
        }
    }

    function getCustomerLabel(code: number): string {
        const customer = customerCodes.find(c => c.value === code);
        return customer?.label.split(' - ')[1] || '';
    }
    
    function handleModeFormCancel() {
        selectedMode = null;
        // operatorDataLoaded = false;
    }
</script>

<main class="schedule-page">
    <header class="schedule-header">
        {#if creationMode === 'operator' && !operatorDataLoaded}
            <CreationHeader
                onCancel={handleCreationCancel}
                onDataProcessed={updateIntervalsFromOperatorData}
            />
        {:else}
            <FileMenu 
                {userData}
                onOperatorCreate={startOperatorCreation}
                onReferenceCreate={startReferenceCreation}
            />
        {/if}
    </header>
    
    <div class="grid-container">
        <ScheduleGrid 
            {intervals}
            {workModes}
            onModeSelect={handleModeSelect}
        />
    </div>

    {#if selectedMode}
        <div class="creation-form-container">
            <ModeCreationFormComponent
                {selectedMode}
                bind:formData={currentFormData}
                onSubmit={handleModeFormSubmit}
                onCancel={handleModeFormCancel}
            />
        </div>
    {/if}
    
    <footer class="schedule-footer">
        <CityLegend {cities} />
    </footer>
</main>

<style>
    .schedule-page {
        display: flex;
        flex-direction: column;
        height: 100vh;
        background: #f5f7fa;
        overflow: auto;
    }

    .schedule-header {
        display: flex;
        align-items: center;
        padding: 0.5rem 1rem;
        background: white;
        border-bottom: 1px solid #e1e5e9;
        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        flex-shrink: 0;
    }
    
    .grid-container {
        padding: 0;
        width: 100%;
        flex: 1;
        /* overflow: auto; */
        display: flex;
        justify-content: center;
        align-items: flex-start;
    }

    .schedule-footer {
        display: flex;
        justify-content: end;
        width: 100%;
        padding: 0.5rem 2rem;
        flex-shrink: 0;
        background: white;
        border-top: 1px solid #e1e5e9;
    }

    .creation-form-container {
        flex: 1;
        padding: 1rem 2rem;
        background: #f5f7fa;
    }
</style>
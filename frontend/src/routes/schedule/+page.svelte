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
        { id: 'moscow', name: 'Москва', color: '#4299e1' },
        { id: 'novosibirsk', name: 'Новосибирск', color: '#48bb78' },
        { id: 'vladivostok', name: 'Владивосток', color: '#ed8936' },
        { id: 'moscow2', name: 'Москва2', color: '#4399e1' },
        { id: 'novosibirsk2', name: 'Новосибирск2', color: '#44bb78' },
        { id: 'vladivostok2', name: 'Владивосток2', color: '#ed8636' }
    ];
    
    const workModes: WorkMode[] = [
        { id: 'mode_1', label: 'Астрокорр.', order: '0' },
        { id: 'mode_2', label: 'Съемки', order: '1' },
        { id: 'mode_3', label: 'Распр. ОМИ', order: '2' },
        { id: 'mode_4', label: 'Режимы ТНП', order: '3' },
        { id: 'mode_5', label: 'Калибр. ВД', order: '4' },
        { id: 'mode_6', label: 'Техн. съемки', order: '5' },
        { id: 'mode_7', label: 'Юстировки ОНА', order: '6' }
    ];
    
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

    let selectedMode = $state<string | null>(null);
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
    
    function startOperatorCreation() {
        creationMode = 'operator';
    }
    
    function startReferenceCreation() {
        creationMode = 'reference';
        console.log('Начинаем создание ПРЦА по опорной ПРЦА');
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
        
        const newIntervals = ScheduleCreationService.convertToTimeIntervals(
            newOperatorData,
            newPpiAssignments,
            workModes
        );
        
        console.log('Созданы интервалы для сетки:', newIntervals);
        intervals = newIntervals;
    }

    function handleModeSelect(modeId: string) {
        let formModeType: 'kvd' | 'tnp' | 'ts' | 's' | 'omi' | 'ona' | 'astr' | null = null;
        let defaultDuration = 300;
        
        switch(modeId) {
            case 'mode_1': formModeType = 'astr'; defaultDuration = 300; break;
            case 'mode_2': formModeType = 's'; defaultDuration = 420; break;
            case 'mode_3': formModeType = 'omi'; defaultDuration = 60; break;
            case 'mode_4': formModeType = 'tnp'; defaultDuration = 516; break;
            case 'mode_5': formModeType = 'kvd'; defaultDuration = 420; break;
            case 'mode_6': formModeType = 'ts'; defaultDuration = 420; break;
            case 'mode_7': formModeType = 'ona'; defaultDuration = 60; break;
            default:
                console.warn('Unknown mode selected:', modeId);
                return;
        }
        
        selectedMode = modeId;
        currentFormData = {
            modeType: formModeType,
            ppiNum: 1,
            duration: defaultDuration,
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
        };
        
    }

    function handleModeFormSubmit(formData: ModeCreationForm) {
        const tempId = `created_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        
        const modeData = createProgramModeData(formData, tempId);
        
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
        const baseData = {
            numRp: 0,
            numKa: operatorData?.main.nKa || 1,
            dateOn: calculateDateFromTime(formData.startTime),
            dateOff: calculateEndDate(formData.startTime, formData.duration),
            kodMode: getKodMode(formData.modeType!),
            numPpi: formData.ppiNum,
            dlit: formData.duration,
            zakazchik: getCustomerLabel(formData.customerCode)
        };
        
        if (formData.modeType === 'kvd') {
            return {
                ...baseData,
                kvdData: {
                    id: 0,
                    idMain: 0,
                    dn: baseData.dateOn,
                    dk: baseData.dateOff,
                    prMsu: formData.msu1Vd.length > 0 ? 1 : 0,
                    prBssd: formData.msu2Vd.length > 0 ? 1 : 0,
                    prZg: 0
                }
            };
        } else if (formData.modeType === 'ts') {
            return {
                ...baseData,
                tsData: {
                    id: 0,
                    idMain: 0,
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
        } else if (formData.modeType === 'tnp') {
            return {
                ...baseData,
                tnpData: {
                    id: 0,
                    idMain: 0,
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
        const modeId = getModeIdForInterval(formData.modeType!);
        
        const interval = {
            id: tempId,
            mode: modeId,
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
    
    function getKodMode(modeType: string): number {
        switch(modeType) {
            case 'astr': return 1; // Астрокоррекции
            case 's': return 2;    // Съемки
            case 'omi': return 3;  // Распр. ОМИ (но для ОМИ обычно отдельный код)
            case 'tnp': return 4;
            case 'kvd': return 3;  // Калибровка ВД (обычно тот же код что и ОМИ)
            case 'ts': return 5;
            case 'ona': return 6;  // Юстировки ОНА
            default: return 0;
        }
    }
    
    function getModeIdForInterval(modeType: string | null): string {
        if (!modeType) return 'mode_1';
        
        switch(modeType) {
            case 'astr': return 'mode_1';
            case 's': return 'mode_2';
            case 'omi': return 'mode_3';
            case 'tnp': return 'mode_4';
            case 'kvd': return 'mode_5';
            case 'ts': return 'mode_6';
            case 'ona': return 'mode_7';
            case 'mode_1': return 'mode_1';
            case 'mode_2': return 'mode_2';
            case 'mode_3': return 'mode_3';
            case 'mode_4': return 'mode_4';
            case 'mode_5': return 'mode_5';
            case 'mode_6': return 'mode_6';
            case 'mode_7': return 'mode_7';
            default: return 'mode_1';
        }
    }
        
    function getModeTitle(modeType: string): string {
        switch(modeType) {
            case 'astr': return 'Астрокоррекции';
            case 's': return 'Съемки';
            case 'omi': return 'Распр. ОМИ';
            case 'tnp': return 'Режим ТНП';
            case 'kvd': return 'Калибровка ВД';
            case 'ts': return 'Технологическая съемка';
            case 'ona': return 'Юстировки ОНА';
            default: return 'Режим';
        }
    }

    function getCustomerLabel(code: number): string {
        const customer = customerCodes.find(c => c.value === code);
        return customer?.label.split(' - ')[1] || '';
    }
    
    function handleModeFormCancel() {
        selectedMode = null;
    }
</script>

<main class="schedule-page">
    <header class="schedule-header">
        {#if creationMode === 'operator'}
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
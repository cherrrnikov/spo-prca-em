<script lang="ts">
    import CityLegend from "$lib/components/CityLegend.svelte";
    import FileMenu from "$lib/components/FileMenu.svelte";
    import ScheduleGrid from "$lib/components/ScheduleGrid.svelte";
    import type { UserResponse } from "$lib/types/auth";
    import { onMount } from "svelte";

    let isLoading = $state(true);
    let userData = $state<UserResponse | null>(null);

    const workModes = [
        {id: 'mode_1', label: 'Астрокоррекции', order: 0},
        {id: 'mode_2', label: 'Штатные съемки', order: 1},
        {id: 'mode_3', label: 'Учащенные съемки', order: 2},
        {id: 'mode_4', label: 'Распростра. ОМИ', order: 3},
        {id: 'mode_5', label: 'Ретрансляции МЦИ', order: 4},
        {id: 'mode_6', label: 'Режимы ТНП', order: 5},
        {id: 'mode_7', label: 'Калибровки ВД', order: 6},
        {id: 'mode_8', label: 'Техн. съемки', order: 7},
        {id: 'mode_9', label: 'Юстировки МСУ ГС', order: 8},
        {id: 'mode_10', label: 'Юстировки ОНА', order: 9}
    ];

    const cities = [
        {id: 'moscow', name: 'Москва', color: '#4299e1'},
        {id: 'novosibirsk', name: 'Новосибирск', color: '#48bb78'},
        {id: 'vladivostok', name: 'Владивосток', color: '#ed8936'},
        {id: 'moscow2', name: 'Москва2', color: '#4399e1'},
        {id: 'novosibirsk2', name: 'Новосибирск2', color: '#44bb78'},
        {id: 'vladivostok2', name: 'Владивосток2', color: '#ed8636'}
    ];

    const intervals = [
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
            title: 'Интервал 4'
        }
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

        setTimeout(() => {
            isLoading = false;
        }, 300);
    });
</script>

<main class="schedule-page">
    <header class="schedule-header">
        <FileMenu {userData}/>
        
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
        background: white;
        border-top: 1px solid #e1e5e9;
        padding: 0.5rem 2rem;
    }


</style>
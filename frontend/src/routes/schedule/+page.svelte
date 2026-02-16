<script lang="ts">
    import CityLegend from '$lib/components/CityLegend.svelte';
    import FileMenu from '$lib/components/FileMenu.svelte';
    import ScheduleGrid from '$lib/components/ScheduleGrid.svelte';
    import {
    	CITIES,
    	CUSTOMER_CODES,
    	WORK_MODES
    } from '$lib/constants/schedule';
    import { useScheduleState } from '$lib/hooks/useScheduleState';
    import type { OperatorData, PpiAssignment, TimeInterval } from '$lib/types/schedule';
    import { AstrocorrectionService } from '$lib/utils/astrocorrection.service';
    import { IntervalUtils } from '$lib/utils/interval';
    import { TimeUtils } from '$lib/utils/time';
    import { onMount } from 'svelte';
    import CreationHeader from '../../features/schedule-creation/components/CreationHeader.svelte';
    import ModeCreationFormComponent from '../../features/schedule-creation/components/ModeCreationForm.svelte';
    import { ScheduleApiService } from '../../features/services/api/scheduleApi.service';
    import { ScheduleCreationService } from '../../features/services/scheduleCreation.service';

    const cities = CITIES;
    const workModes = WORK_MODES;
    const customerCodes = CUSTOMER_CODES;

    const {
        userData,
        creationMode,
        intervals,
        operatorData,
        ppiAssignments,
        operatorDataLoaded,
        selectedProgramDate,
        forecastData,
        shadowIntervals,
        zasvetkaIntervals,
        forecastDataLoaded,
        selectedMode,
        createdPrograms,
        editingInterval,
        selectedIntervalId,
        contextDate,   
        hasAstrocorrectionData,    
        vkiIntervals,
        rotationIntervals,
        isEditing,  
        
        loadUserData,
        handleIntervalClick,
        handleIntervalDelete,
        handleIntervalUpdate,
        handleModeSelect,
        handleModeFormSubmit,
        handleModeFormCancel,
        loadAstroEvents,
        checkAndAddAstrocorrection,
        
        getIntervalColor,
        getIntervalTitle,
        setContextDate
    } = useScheduleState();

    onMount(() => {
        loadUserData();
    });

    function startOperatorCreation() {
        creationMode.set('operator');
    }
    
    function startReferenceCreation() {
        creationMode.set('reference');
        alert('Создание по опорной ПРЦА (в разработке)');
    }
    
    function handleCreationCancel() {
        creationMode.set(null);
    }
    
    async function updateIntervalsFromOperatorData(
        newOperatorData: OperatorData,
        newPpiAssignments: PpiAssignment[]
    ) {
        operatorData.set(newOperatorData);
        ppiAssignments.set(newPpiAssignments);
        creationMode.set('operator');
        operatorDataLoaded.set(true);

        if (newOperatorData.main?.d_np) {
            const date = newOperatorData.main.d_np.split('T')[0];
            setContextDate(date); 
            selectedProgramDate.set(date);

            await loadAstroEvents(date);
            console.log(`ВКИ после загрузки:`, $vkiIntervals);
            await loadForecastData(date);
            await checkAndAddAstrocorrection(date);
        }

        const newIntervals = ScheduleCreationService.convertToTimeIntervals(
            newOperatorData,
            newPpiAssignments,
            workModes
        );
        
        const intervalsWithAstro = await addAstrocorrectionToIntervals(newIntervals, $contextDate);
        const currentVkiIntervals = $vkiIntervals;
        const currentZasvetkaIntervals = $zasvetkaIntervals;
        const currentShadowIntervals = $shadowIntervals;

        console.log('Проверка конфликтов с ВКИ:', currentVkiIntervals);

        const intervalsWithConflicts = IntervalUtils.checkAllConflicts(
            intervalsWithAstro, 
            currentZasvetkaIntervals, 
            currentShadowIntervals,
            currentVkiIntervals,
            $rotationIntervals
        );

        intervals.set(intervalsWithConflicts);

        isEditing.set(false);
        logAllIntervals('После загрузки данных оператора');
    }

    async function addAstrocorrectionToIntervals(intervals: TimeInterval[], date: string): Promise<TimeInterval[]> {
        try {
            const hasAstro = await ScheduleApiService.hasAstrocorrectionData(date);
            console.log('Астрокоррекция для даты', date, ':', 
                hasAstro ? 'ПОЛНЫЙ РЕЖИМ (6 интервалов)' : 'ОБЫЧНЫЙ РЕЖИМ (2 интервала)');
            
            return AstrocorrectionService.mergeAstrocorrection(intervals, date, hasAstro);
        } catch (error) {
            console.warn('Ошибка при добавлении астрокоррекции:', error);
            return intervals;
        }
    }

    async function loadForecastData(date: string) {
        try {
            const data = await ScheduleCreationService.loadForecastData(date);
            forecastData.set(data);
            forecastDataLoaded.set(true);
            
            const forecastIntervals = ScheduleCreationService.convertForecastToIntervals(data);
            shadowIntervals.set(forecastIntervals.shadows);
            zasvetkaIntervals.set(forecastIntervals.zasvetki);
            
            console.log('Прогнозные данные загружены:', {
                shadows: forecastIntervals.shadows,
                zasvetki: forecastIntervals.zasvetki
            });

            const currentIntervals = $intervals;
            if (currentIntervals.length > 0) {
                const intervalsWithConflicts = IntervalUtils.checkAllConflicts(
                    currentIntervals, 
                    forecastIntervals.zasvetki
                );
                intervals.set(intervalsWithConflicts);
            }
        } catch (error) {
            console.warn('Ошибка загрузки прогнозных данных:', error);
            shadowIntervals.set([]);
            zasvetkaIntervals.set([]);
        }
    }

    function formatDate(dateString: string): string {
        return TimeUtils.formatDate(dateString);
    }

    function logAllIntervals(action: string) {
        console.log(`${action} - Все интервалы:`, $intervals);
        console.log(`Статистика: Всего: ${$intervals.length}, Сохраняемых: ${$intervals.filter(i => i.willBeSaved).length}`);
    }
</script>

<main class="schedule-page">
    <header class="schedule-header">
        {#if $creationMode === 'operator' && !$operatorDataLoaded}
            <CreationHeader
                onCancel={handleCreationCancel}
                onDataProcessed={updateIntervalsFromOperatorData}
            />
        {:else if $creationMode === 'operator' && $operatorDataLoaded}
            <div class="header-content">
                <FileMenu 
                    userData={$userData}
                    onOperatorCreate={startOperatorCreation}
                    onReferenceCreate={startReferenceCreation}
                    intervals={$intervals}
                    operatorData={$operatorData}
                    ppiAssignments={$ppiAssignments}
                    selectedProgramDate={$selectedProgramDate}
                    createdPrograms={$createdPrograms}
                />
                <div class="program-date-info">
                    <h2 class="program-date-title">
                        Программа работы БЦА действует с <strong>{formatDate($selectedProgramDate)}</strong> 
                        по <strong>{formatDate($selectedProgramDate)} 23:59:59</strong>
                    </h2>
                </div>
            </div>
        {:else}
            <FileMenu 
                userData={$userData}
                onOperatorCreate={startOperatorCreation}
                onReferenceCreate={startReferenceCreation}
            />
        {/if}
    </header>
    
    <div class="grid-container">
        <ScheduleGrid 
            intervals={$intervals}
            shadowIntervals={$shadowIntervals}
            zasvetkaIntervals={$zasvetkaIntervals}
            {workModes}
            vkiIntervals={$vkiIntervals}
            rotationIntervals={$rotationIntervals}
            onModeSelect={handleModeSelect}
            getIntervalColor={getIntervalColor}
            getIntervalTitle={getIntervalTitle}
            onIntervalClick={handleIntervalClick}
            onIntervalDelete={handleIntervalDelete}
            selectedIntervalId={$selectedIntervalId}
            isEditing={$isEditing}
        />
    </div>

    {#if $selectedMode}
        <div class="creation-form-container">
            <ModeCreationFormComponent
                selectedMode={$selectedMode}
                editingInterval={$editingInterval}
                onSubmit={handleModeFormSubmit}
                onCancel={handleModeFormCancel}
                onUpdate={handleIntervalUpdate}
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
    .header-content {
        display: flex;
        align-items: center;
        width: 100%;
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
        max-width: 50%;
    }

    .program-date-info {
        margin-left: 1rem;
        padding-right: 1rem;
    }
    
    .program-date-title {
        font-size: 1rem;
        font-weight: bold;
        color: #2d3748;
        margin: 0;
        text-align: right;
    }
    
    .program-date-title strong {
        font-weight: bold;
        color: #2c5282;
    }
</style>
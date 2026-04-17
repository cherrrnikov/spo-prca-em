<script lang="ts">
    import { onMount } from 'svelte';
    
    import {
    	CITIES,
    	WORK_MODES
    } from '$lib/constants/schedule';
    
    import CityLegend from '$lib/components/CityLegend.svelte';
    import FileMenu from '$lib/components/FileMenu.svelte';
    import ScheduleGrid from '$lib/components/ScheduleGrid.svelte';
    import { useScheduleState } from '$lib/hooks/useScheduleState/index';
    import CreationHeader from '../../features/schedule-creation/components/CreationHeader.svelte';
    import ModeCreationFormComponent from '../../features/schedule-creation/components/ModeCreationForm.svelte';
    
    import { ScheduleApiService } from '../../features/services/api/scheduleApi.service';
    import { ScheduleCreationService } from '../../features/services/scheduleCreation.service';
    
    import { AstrocorrectionService } from '$lib/utils/astrocorrection.service';
    import { TimeUtils } from '$lib/utils/time';
    
    import { modal } from '$lib/services/modal.service';
    import type { OperatorData, PpiAssignment } from '$lib/types';
    import { checkAllConflicts } from '$lib/utils/interval/index';
    import AnalysisModal from '../../features/schedule-creation/components/AnalysisModal.svelte';
    import ProgramsSelector from '../../features/schedule-creation/components/ProgramsSelector.svelte';
    import { ProgramCreatorService } from '../../features/services/data/programCreator.service';
    import { ScheduleConverterService } from '../../features/services/data/scheduleConverter.service';

    const cities = CITIES;
    const workModes = WORK_MODES;

    const {
        userData,
        creationMode,
        intervals,
        operatorData,
        ppiAssignments,
        operatorDataLoaded,
        selectedProgramDate,
        shadowIntervals,
        zasvetkaIntervals,
        selectedMode,
        createdPrograms,
        editingInterval,
        selectedIntervalId,
        contextDate,   
        vkiIntervals,
        rotationIntervals,
        isEditing,  
        programsList,
        activeProgramId,
        isAnalysisMode,
        activeProgramDate,
        analysisModal,
        bortData,
        numKa,
        currentNumRp,
        
        loadUserData,
        handleIntervalClick,
        handleIntervalDelete,
        handleIntervalUpdate,
        handleModeSelect,
        handleModeFormSubmit,
        handleModeFormCancel,
        loadAstroEvents,
        
        getIntervalColor,
        getIntervalTooltip,
        setContextDate,

        // saveCurrentProgramToAnalysis,
        openAnalysisModal,
        closeAnalysisModal,
        createAnalysis,
        selectProgram,
        exitAnalysisMode,
        deleteProgramFromAnalysis,
        cleanupAfterSave,
        isReadOnly,

        updateAllConflicts
    } = useScheduleState();

    onMount(() => {
        loadUserData();
    });

    $effect(() => {
    });

    function resetAndStartOperatorCreation() {
        intervals.set([]);
        operatorData.set(null);
        ppiAssignments.set([]);
        createdPrograms.set([]);
        shadowIntervals.set([]);
        zasvetkaIntervals.set([]);
        vkiIntervals.set([]);
        rotationIntervals.set([]);
        
        operatorDataLoaded.set(false);
        selectedMode.set(null);
        editingInterval.set(null);
        selectedIntervalId.set(null);
        selectedProgramDate.set('');
        
        // Сбрасываем read-only режим
        isReadOnly.set(false);
        
        // Если мы в режиме анализа - выходим из него
        if ($isAnalysisMode) {
            exitAnalysisMode();
        }
        
        creationMode.set('operator');
    }

    function startOperatorCreation() {
        if ($operatorDataLoaded && $intervals.length > 0) {
            modal.confirm(
                'Подтверждение',
                'Текущая ПРЦА не будет сохранена. Продолжить?',
                resetAndStartOperatorCreation,
                undefined,
                'warning'
            );
        } else {
            resetAndStartOperatorCreation();
        }
    }
    
    function startReferenceCreation() {
        creationMode.set('reference');
        modal.alert('Информация', 'Создание по опорной ПРЦА (в разработке)', 'info');
    }
    
    function handleCreationCancel() {
        creationMode.set(null);
        isReadOnly.set(false);
    }
    
    // Загрузка данных
    async function loadForecastData(date: string) {
        try {
            const data = await ScheduleApiService.loadForecastData(date);
            
            const forecastIntervals = ScheduleConverterService.convertForecastToIntervals(data);
            shadowIntervals.set(forecastIntervals.shadows);
            zasvetkaIntervals.set(forecastIntervals.zasvetki);
        } catch (error) {
            console.warn('Ошибка загрузки прогнозных данных:', error);
            shadowIntervals.set([]);
            zasvetkaIntervals.set([]);
        }
    }
    
    // Обработка данных оператора
    async function updateIntervalsFromOperatorData(
        newOperatorData: OperatorData,
        newPpiAssignments: PpiAssignment[]
    ) {
        operatorData.set(newOperatorData);
        ppiAssignments.set(newPpiAssignments);

        if (newOperatorData?.main?.n_ka) {
            numKa.set(newOperatorData.main.n_ka);
        }

        creationMode.set('operator');
        operatorDataLoaded.set(true);

        if (newOperatorData.main?.d_np) {


            const date = newOperatorData.main.d_np.split('T')[0];
            setContextDate(date); 

            const bortDataForDate = await ScheduleApiService.loadBortData(date);
            bortData.set(bortDataForDate);

            selectedProgramDate.set(date);

            await Promise.all([
                loadAstroEvents(date),
                loadForecastData(date)
            ]);

            const hasAstro = await ScheduleApiService.hasAstrocorrectionData(date);
            
            // Создаём интервалы из ИД06
            const newIntervals = ScheduleCreationService.convertToTimeIntervals(
                newOperatorData,
                newPpiAssignments,
                workModes,
                undefined,
                bortDataForDate
            );
            
            // СОЗДАЁМ createdPrograms ДЛЯ ВСЕХ ИНТЕРВАЛОВ ИЗ ИД06
            const newCreatedPrograms = ProgramCreatorService.createProgramsFromOperatorData(
                newOperatorData,
                newPpiAssignments,
                date,
                newIntervals,
                bortDataForDate
            );
            
            // Добавляем астрокоррекции
            const intervalsWithAstro = AstrocorrectionService.mergeAstrocorrection(
                newIntervals, 
                date, 
                hasAstro
            );

            // Проверяем конфликты
            const intervalsWithConflicts = checkAllConflicts(
                intervalsWithAstro, 
                $zasvetkaIntervals, 
                $shadowIntervals,
                $vkiIntervals,
                $rotationIntervals
            );

            intervals.set(intervalsWithConflicts);
            createdPrograms.set(newCreatedPrograms); 
            isEditing.set(false);
        }
    }

    function logAllIntervals(action: string) {
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
                    onAnalysisClick={openAnalysisModal}
                    isAnalysisMode={$isAnalysisMode}
                    isOperatorMode={$creationMode === 'operator'}
                    intervals={$intervals}
                    operatorData={$operatorData}
                    ppiAssignments={$ppiAssignments}
                    selectedProgramDate={$selectedProgramDate}
                    createdPrograms={$createdPrograms}
                    programsList={$programsList}
                    updateAllConflicts={updateAllConflicts} 
                    numKa={$numKa}
                    onAfterSave={cleanupAfterSave}
                    onNumRpSaved={(numRp) => {
                        currentNumRp.set(numRp);
                    }}
                />
                <div class="program-date-info">
                    <h2 class="program-date-title">
                        {#if $isReadOnly}
                            ПРЦА действует с 
                            <strong>{TimeUtils.formatDate($selectedProgramDate)} 00:00:00</strong> 
                            по <strong>{TimeUtils.formatDate($selectedProgramDate)} 23:59:59</strong>
                            {#if $operatorData?.main?.n_ka}
                                <span class="program-details">(КА {$operatorData.main.n_ka}, РП {$currentNumRp})</span>
                            {/if}
                        {:else}
                            ПРЦА действует с 
                            <strong>{TimeUtils.formatDate($selectedProgramDate)} 00:00:00</strong> 
                            по <strong>{TimeUtils.formatDate($selectedProgramDate)} 23:59:59</strong>
                        {/if}
                    </h2>
                </div>
            </div>
        {:else}
        <FileMenu 
            userData={$userData}
            onOperatorCreate={startOperatorCreation}
            onReferenceCreate={startReferenceCreation}
            onAnalysisClick={openAnalysisModal}
            isAnalysisMode={$isAnalysisMode}
            isOperatorMode={$creationMode === 'operator'}
            intervals={$intervals}
            operatorData={$operatorData}
            ppiAssignments={$ppiAssignments}
            selectedProgramDate={$selectedProgramDate}
            createdPrograms={$createdPrograms}
            programsList={$programsList}
            updateAllConflicts={updateAllConflicts} 
            numKa={$numKa}
            onAfterSave={cleanupAfterSave}
            onNumRpSaved={(numRp) => {
                currentNumRp.set(numRp);
            }}
        />
        {#if $selectedProgramDate}
            <div class="program-date-info">
                <h2 class="program-date-title">
                    {#if $isReadOnly}
                        ПРЦА действует с 
                        <strong>{TimeUtils.formatDate($selectedProgramDate)} 00:00:00</strong> 
                        по <strong>{TimeUtils.formatDate($selectedProgramDate)} 23:59:59</strong>
                        {#if $operatorData?.main?.n_ka}
                            <span class="program-details">(Номер КА: {$operatorData.main.n_ka}, Номер РП: {$currentNumRp})</span>
                        {/if}
                    {:else}
                        ПРЦА действует с 
                        <strong>{TimeUtils.formatDate($selectedProgramDate)} 00:00:00</strong> 
                        по <strong>{TimeUtils.formatDate($selectedProgramDate)} 23:59:59</strong>
                    {/if}
                </h2>
            </div>
        {/if}
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
            getIntervalTooltip={getIntervalTooltip}
            onIntervalClick={handleIntervalClick}
            onIntervalDelete={handleIntervalDelete}
            selectedIntervalId={$selectedIntervalId}
            isEditing={$isEditing}
            isReadOnly={$isReadOnly}
            operatorDataLoaded={$operatorDataLoaded}
        />
    </div>

    <div class="forms-container">
        {#if $selectedMode}
            <ModeCreationFormComponent
                selectedMode={$selectedMode}
                editingInterval={$editingInterval}
                onSubmit={handleModeFormSubmit}
                onCancel={handleModeFormCancel}
                onUpdate={handleIntervalUpdate}
                contextDate={$contextDate}
                bortData={$bortData}
            />
        {/if}

        {#if $isAnalysisMode}
            <ProgramsSelector
                programs={$programsList}
                activeId={$activeProgramId}
                onSelect={selectProgram}
                onExitAnalysis={exitAnalysisMode}
                onDelete={deleteProgramFromAnalysis}
            />
        {/if}
    </div>

    <AnalysisModal
        modalData={$analysisModal}
        onClose={closeAnalysisModal}
        onCreate={createAnalysis}
        contextDate={$selectedProgramDate}
    />

    
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
        overflow: hidden;
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
        flex: 0 1 auto;
        display: flex;
        justify-content: center;
        align-items: flex-start;
        overflow-y: auto;
        min-height: 0;
    }

    .forms-container {
        flex: 1 1 auto;  
        min-height: 0;
        flex-shrink: 0;
        border-top: 1px solid #e2e8f0;
        padding: 1rem;
        max-height: 40vh;
        overflow-y: auto;
        display: flex;
        gap: 2rem;
    }
    .schedule-footer {
        display: flex;
        justify-content: end;
        width: 100%;
        padding: 0.5rem 2rem;
        flex-shrink: 0;
        background: white;
        border-top: 1px solid #e1e5e9;
        margin-top: auto;
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
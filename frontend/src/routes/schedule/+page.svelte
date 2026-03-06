<script lang="ts">
    import { onMount } from 'svelte';
    
    import {
    	CITIES,
    	CUSTOMER_CODES,
    	WORK_MODES
    } from '$lib/constants/schedule';
    
    import CityLegend from '$lib/components/CityLegend.svelte';
    import FileMenu from '$lib/components/FileMenu.svelte';
    import ScheduleGrid from '$lib/components/ScheduleGrid.svelte';
    import CreationHeader from '../../features/schedule-creation/components/CreationHeader.svelte';
    import ModeCreationFormComponent from '../../features/schedule-creation/components/ModeCreationForm.svelte';
    
    import { useScheduleState } from '$lib/hooks/useScheduleState/index';
    
    import { ScheduleApiService } from '../../features/services/api/scheduleApi.service';
    import { ScheduleCreationService } from '../../features/services/scheduleCreation.service';
    
    import { AstrocorrectionService } from '$lib/utils/astrocorrection.service';
    import { TimeUtils } from '$lib/utils/time';
    
    import type { CreatedProgramData, OperatorData, PpiAssignment, ProgramModeData, TimeInterval } from '$lib/types';
    import { checkAllConflicts } from '$lib/utils/interval/index';
    import { ModeUtils } from '$lib/utils/mode';
    import { get } from 'svelte/store';
    import AnalysisModal from '../../features/schedule-creation/components/AnalysisModal.svelte';
    import ProgramsSelector from '../../features/schedule-creation/components/ProgramsSelector.svelte';

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
        deleteProgramFromAnalysis
    } = useScheduleState();

    onMount(() => {
        loadUserData();
    });

    $effect(() => {
        console.log("creationMode изменился:", $creationMode);
        console.log("operatorDataLoaded:", $operatorDataLoaded);
        console.log("intervals length:", $intervals.length);
    });

    // Создание ПРЦА
    function startOperatorCreation() {
        // Проверяем, есть ли уже загруженные данные
        if ($operatorDataLoaded && $intervals.length > 0) {
            const confirm = window.confirm(
                'Текущая ПРЦА не будет сохранена. Продолжить?'
            );
            if (!confirm) return;
            
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
        }
        
        // Если мы в режиме анализа - выходим из него
        if ($isAnalysisMode) {
            exitAnalysisMode();
        }
        
        creationMode.set('operator');
    }
    
    function startReferenceCreation() {
        creationMode.set('reference');
        alert('Создание по опорной ПРЦА (в разработке)');
    }
    
    function handleCreationCancel() {
        creationMode.set(null);
    }
    
    // Загрузка данных
    async function loadForecastData(date: string) {
        try {
            const data = await ScheduleCreationService.loadForecastData(date);
            
            const forecastIntervals = ScheduleCreationService.convertForecastToIntervals(data);
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
        creationMode.set('operator');
        operatorDataLoaded.set(true);

        if (newOperatorData.main?.d_np) {
            const date = newOperatorData.main.d_np.split('T')[0];
            setContextDate(date); 
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
                workModes
            );
            
            // СОЗДАЁМ createdPrograms ДЛЯ ВСЕХ ИНТЕРВАЛОВ ИЗ ИД06
            const newCreatedPrograms = createProgramsFromOperatorData(
                newOperatorData,
                newPpiAssignments,
                date,
                newIntervals
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

    // Функция создания createdPrograms из ИД06
    function createProgramsFromOperatorData(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        date: string,
        intervals: TimeInterval[]
    ): CreatedProgramData[] {
        const programs: CreatedProgramData[] = [];
        const mainId = operatorData.main.id;
        const numKa = operatorData.main.n_ka;
        
        // КВД
        if (operatorData.kvd_list) {
            operatorData.kvd_list.forEach((kvd: any) => {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === kvd.id && a.recordType === 'kvd'
                );
                
                if (assignment) {
                    const modeData: ProgramModeData = {
                        numRp: 0,
                        numKa: numKa,
                        dateOn: kvd.dn,
                        dateOff: kvd.dk,
                        kodMode: 7,
                        numPpi: assignment.ppiNum,
                        dlit: TimeUtils.calculateDuration(kvd.dn, kvd.dk),
                        zakazchik: ModeUtils.getCustomerLabel(CUSTOMER_CODES, 1),
                        kvdData: {
                            id: kvd.id,
                            idMain: mainId,
                            prMsu: kvd.pr_msu,
                            prBssd: kvd.pr_bssd,
                            prZg: kvd.pr_zg
                        }
                    };
                    
                    const timeInterval = intervals.find(i => 
                        i.id === `kvd_${kvd.id}`
                    );
                    
                    if (timeInterval) {
                        timeInterval.customerCode = 1;

                        timeInterval.kvdConfig = {
                            prMsu: kvd.pr_msu,
                            prBssd: kvd.pr_bssd,
                            prZg: kvd.pr_zg
                        };
                        
                        programs.push({
                            tempId: `kvd_${kvd.id}`,
                            modeData,
                            timeInterval
                        });
                    }
                }
            });
        }
        
        // ТНП
        if (operatorData.tnp_list) {
            operatorData.tnp_list.forEach((tnp: any) => {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === tnp.id && a.recordType === 'tnp'
                );
                
                if (assignment) {
                    const modeData: ProgramModeData = {
                        numRp: 0,
                        numKa: numKa,
                        dateOn: tnp.dn,
                        dateOff: tnp.dk,
                        kodMode: 4,
                        numPpi: assignment.ppiNum,
                        dlit: tnp.dlit,
                        zakazchik: ModeUtils.getCustomerLabel(CUSTOMER_CODES, 1),
                        tnpData: {
                            id: tnp.id,
                            idMain: mainId,
                            prMsu: 1,  // заглушка
                            prBssd: 1,  // заглушка
                            prZg: 1     // заглушка
                        }
                    };
                    
                    const timeInterval = intervals.find(i => 
                        i.id === `tnp_${tnp.id}`
                    );
                    
                    if (timeInterval) {
                        timeInterval.customerCode = 1;
                        programs.push({
                            tempId: `tnp_${tnp.id}`,
                            modeData,
                            timeInterval
                        });
                    }
                }
            });
        }
        
        // ТС
        if (operatorData.ts_list) {
            operatorData.ts_list.forEach((ts: any) => {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === ts.id && a.recordType === 'ts'
                );
                
                if (assignment) {
                    // Находим все подынтервалы для этого ТС
                    const tsSubIntervals = intervals.filter(i => 
                        i.id.startsWith(`ts_${ts.id}`)
                    );

                    const currentBortData = get(bortData);
                    
                    tsSubIntervals.forEach((subInterval, idx) => {
                        subInterval.customerCode = 1;

                        if (!subInterval.tsData) {
                            subInterval.tsData = {
                                id: 0,
                                idMain: 0,
                                tip: 0,
                                reg: 0,
                                dlit: 0,
                                prMsu1: 0,
                                vd1Msu1: 0,
                                vd2Msu1: 0,
                                vd3Msu1: 0,
                                ik4Msu1: 0,
                                ik5Msu1: 0,
                                ik6Msu1: 0,
                                ik7Msu1: 0,
                                ik8Msu1: 0,
                                ik9Msu1: 0,
                                ik10Msu1: 0,
                                prMsu2: 0,
                                vd1Msu2: 0,
                                vd2Msu2: 0,
                                vd3Msu2: 0,
                                ik4Msu2: 0,
                                ik5Msu2: 0,
                                ik6Msu2: 0,
                                ik7Msu2: 0,
                                ik8Msu2: 0,
                                ik9Msu2: 0,
                                ik10Msu2: 0,
                                prBssd: 0,
                                prZg: 0,
                                prOtklZgBssd: 0
                            };
                        }

                        subInterval.tsData.prBssd = currentBortData?.pr_bssd ?? 0;
                        subInterval.tsData.prZg = currentBortData?.pr_zg ?? 0;
                        subInterval.tsData.prOtklZgBssd = ts.pr_otkl_zg;

                        const modeData: ProgramModeData = {
                            numRp: 0,
                            numKa: numKa,
                            dateOn: `${date}T${subInterval.startTime}`,
                            dateOff: `${date}T${subInterval.endTime}`,
                            kodMode: 8,
                            numPpi: assignment.ppiNum,
                            dlit: subInterval.dlit || 420,
                            zakazchik: ModeUtils.getCustomerLabel(CUSTOMER_CODES, 1),
                            tsData: {
                                id: ts.id,
                                idMain: mainId,
                                tip: ts.tip,
                                reg: ts.reg,
                                dlit: subInterval.dlit || 420,
                                prMsu1: ts.pr_msu1,
                                vd1Msu1: ts.pr_vd1_1,
                                vd2Msu1: ts.pr_vd2_1,
                                vd3Msu1: ts.pr_vd3_1,
                                ik4Msu1: ts.pr_ik4_1,
                                ik5Msu1: ts.pr_ik5_1,
                                ik6Msu1: ts.pr_ik6_1,
                                ik7Msu1: ts.pr_ik7_1,
                                ik8Msu1: ts.pr_ik8_1,
                                ik9Msu1: ts.pr_ik9_1,
                                ik10Msu1: ts.pr_ik10_1,
                                prMsu2: ts.pr_msu2,
                                vd1Msu2: ts.pr_vd1_2,
                                vd2Msu2: ts.pr_vd2_2,
                                vd3Msu2: ts.pr_vd3_2,
                                ik4Msu2: ts.pr_ik4_2,
                                ik5Msu2: ts.pr_ik5_2,
                                ik6Msu2: ts.pr_ik6_2,
                                ik7Msu2: ts.pr_ik7_2,
                                ik8Msu2: ts.pr_ik8_2,
                                ik9Msu2: ts.pr_ik9_2,
                                ik10Msu2: ts.pr_ik10_2,

                                // ИД02
                                prBssd: subInterval.tsData.prBssd,
                                prZg: subInterval.tsData.prZg,

                                // ИД06
                                prOtklZgBssd: subInterval.tsData.prOtklZgBssd
                            }
                        };

                        console.log('tsData:', modeData.tsData);
                        console.log('ИД02 данные:', {
                            prBssd: currentBortData?.pr_bssd,
                            prZg: currentBortData?.pr_zg
                        });
                        
                        programs.push({
                            tempId: `ts_${ts.id}_${idx}`,
                            modeData,
                            timeInterval: subInterval
                        });
                    });
                }
            });
        }
        
        // ОНА
        if (operatorData.ona_list) {
            operatorData.ona_list.forEach((ona: any) => {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === ona.id && a.recordType === 'ona'
                );
                
                if (assignment) {
                    const modeData: ProgramModeData = {
                        numRp: 0,
                        numKa: numKa,
                        dateOn: ona.dn,
                        dateOff: ona.dk,
                        kodMode: 6,
                        numPpi: assignment.ppiNum,
                        dlit: ona.dlit,
                        zakazchik: ModeUtils.getCustomerLabel(CUSTOMER_CODES, 1),
                        onaData: {
                            id: ona.id,
                            idMain: ona.id_main,
                            typeOmi: 1,
                            dN: ona.dn,
                            dK: ona.dk,
                            nOna: ona.n_ona,
                            nPpi: assignment.ppiNum
                        }
                    };
                    
                    const timeInterval = intervals.find(i => 
                        i.id === `ona_${ona.id}`
                    );
                    
                    if (timeInterval) {
                        timeInterval.customerCode = 1;
                        programs.push({
                            tempId: `ona_${ona.id}`,
                            modeData,
                            timeInterval
                        });
                    }
                }
            });
        }
        
        return programs;
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
                    onAnalysisClick={openAnalysisModal}
                    isAnalysisMode={$isAnalysisMode}
                    isOperatorMode={$creationMode === 'operator'}
                    intervals={$intervals}
                    operatorData={$operatorData}
                    ppiAssignments={$ppiAssignments}
                    selectedProgramDate={$selectedProgramDate}
                    createdPrograms={$createdPrograms}
                />
                <div class="program-date-info">
                    <h2 class="program-date-title">
                        {#if $isAnalysisMode && $activeProgramId}
                            Программа работы БЦА действует с 
                            <strong>{formatDate($activeProgramDate)}</strong> 
                            по <strong>{formatDate($activeProgramDate)} 23:59:59</strong>
                        {:else}
                            Программа работы БЦА действует с 
                            <strong>{formatDate($selectedProgramDate)}</strong> 
                            по <strong>{formatDate($selectedProgramDate)} 23:59:59</strong>
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
            getIntervalTooltip={getIntervalTooltip}
            onIntervalClick={handleIntervalClick}
            onIntervalDelete={handleIntervalDelete}
            selectedIntervalId={$selectedIntervalId}
            isEditing={$isEditing}
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
        flex: 0 1 auto;
        display: flex;
        justify-content: center;
        align-items: flex-start;
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
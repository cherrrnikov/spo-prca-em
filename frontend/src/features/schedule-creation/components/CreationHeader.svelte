<script lang="ts">
	import type { OperatorData, Ppi, PpiAssignment, PpiSelectionModal as PpiSelectionModalType } from "$lib/types/schedule";
	import { onMount } from "svelte";
	import { ScheduleCreationService } from "../../services/scheduleCreation.service";
	import type { MsuGsType, ScheduleStatus, ShootingMode } from "../types";
	import CreationForm from "./CreationForm.svelte";
	import PpiSelectionModal from "./PpiSelectionModal.svelte";

    const PPI_LIST: Ppi[] = [
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

    let { onCancel: onCancelProp, onDataProcessed } = $props<{
		onCancel: () => void;
        onDataProcessed?: (operatorData: OperatorData, ppiAssignments: PpiAssignment[]) => void;
	}>();

    let isLoading = $state(false);

    let scheduleStatus = $state<ScheduleStatus>('main');
    let selectedDate = $state('');
    let selectedTime = $state('');
    let shootingMode = $state<ShootingMode>('default');
    let msuGsType = $state<MsuGsType>('msu_gs_1');

    let operatorData = $state<OperatorData | null>(null);
    let ppiAssignments = $state<PpiAssignment[]>([]);

    let ppiModal = $state<PpiSelectionModalType>({
        isOpen: false,
        currentRecord: null,
        recordType: null,
        recordIndex: 0,
        totalRecords: 0,
        selectedPpi: null,
        recordTitle: ''
    });

    onMount(() => {
        const now = new Date();
        selectedDate = now.toISOString().split('T')[0];
        selectedTime = now.toTimeString().substring(0, 5);
    })

    async function handleFormSubmit() {
        if (!selectedDate) {
            alert('Выберите дату');
            return;
        }

        isLoading = true;

        try {
            operatorData = await ScheduleCreationService.loadOperatorData(selectedDate);
            console.log('Данные оператора получены:', operatorData);

            await processAllRecords();
        } catch (error) {
            console.error('Ошибка при загрузке данных:', error);
            alert('Ошибка при загрузке данных: ' + (error as Error).message);
        } finally {
            isLoading = false;
        }
    }

    async function processAllRecords() {
        if (!operatorData) return;
        
        if (operatorData.kvdList && operatorData.kvdList.length > 0) {
            await processRecordBatch(operatorData.kvdList, 'kvd', 'Калибровка ВД');
        }
        
        if (operatorData.tnpList && operatorData.tnpList.length > 0) {
            await processRecordBatch(operatorData.tnpList, 'tnp', 'Режим ТНП');
        }
        
        if (operatorData.tsList && operatorData.tsList.length > 0) {
            await processRecordBatch(operatorData.tsList, 'ts', 'Технологическая съемка');
        }
        
        
        const stats = ScheduleCreationService.getAssignmentStatistics(operatorData, ppiAssignments);
        
        handleCancelCreation();
    }
    
    async function processRecordBatch(
        records: any[],
        recordType: 'kvd' | 'tnp' | 'ts',
        recordTitle: string
    ) {
        for (let i = 0; i < records.length; i++) {
            const record = records[i];
            
            const existingAssignment = ppiAssignments.find(a => 
                a.recordId === record.id && a.recordType === recordType
            );
            
            if (existingAssignment) {
                console.log(`ППИ уже выбран для ${recordType.toUpperCase()} записи ${record.id}`);
                continue;
            }
            
            await showPpiModal(record, recordType, i, records.length, recordTitle);
        }
    }
    
    async function showPpiModal(
        record: any,
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
    
    function handleApplyPpi(record: any, ppi: Ppi, applyToAll: boolean) {
        if (applyToAll && ppiModal.recordType && operatorData) {
            let records: any[] = [];
            
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
            
        } else {
            addPpiAssignment(record.id, ppiModal.recordType!, ppi);
        }
        
        ppiModal.isOpen = false;
    }
    
    function addPpiAssignment(
        recordId: number,
        recordType: 'kvd' | 'tnp' | 'ts',
        ppi: Ppi
    ) {
        ppiAssignments = ppiAssignments.filter(a => 
            !(a.recordId === recordId && a.recordType === recordType)
        );
        
        ppiAssignments = [
            ...ppiAssignments,
            {
                recordId,
                recordType,
                ppiId: ppi.id,
                ppiNum: ppi.numPpi
            }
        ];
    }
    
    function closePpiModal() {
        ppiModal.isOpen = false;
    }
    
    function handleCancelCreation() {
        if (operatorData && ppiAssignments.length > 0 && onDataProcessed) {
            onDataProcessed(operatorData, ppiAssignments);
        }

        scheduleStatus = 'main';
        shootingMode = 'default';
        msuGsType = 'msu_gs_1';
        operatorData = null;
        ppiAssignments = [];
        ppiModal.isOpen = false;
        isLoading = false;
        
        // Это вызовет onCancel в +page.svelte и вернет FileMenu
        onCancelProp?.();
    }
</script>

<div class="creation-form-container">
    <CreationForm
        bind:scheduleStatus
        bind:selectedDate
        bind:selectedTime
        bind:shootingMode
        bind:msuGsType
        {isLoading}
        onSubmit={handleFormSubmit}
        onCancel={handleCancelCreation}
    />
</div>

<PpiSelectionModal
    modalData={ppiModal}
    ppiList={PPI_LIST}
    closeModal={closePpiModal}
    applyPpi={handleApplyPpi}
/>

<style>
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
        padding: 1rem 1.5rem;
        background: #f8fafc;
        border-bottom: 1px solid #e2e8f0;
    }

    .creation-title {
        font-size: 1.1rem;
        font-weight: 600;
        color: #2d3748;
    }

    .creation-status {
        background: #4299e1;
        color: white;
        padding: 0.5rem 1rem;
        border-radius: 4px;
        font-size: 0.9rem;
        font-weight: 500;
    }
</style>
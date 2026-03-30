<script lang="ts">
	import type { OperatorData, Ppi, PpiAssignment, PpiSelectionModal as PpiSelectionModalType } from "$lib/types";
	import { onMount } from "svelte";
	import { ScheduleCreationService } from "../../services/scheduleCreation.service";
	import type { ScheduleStatus } from "../types";
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

    let { onCancel: onCancelProp, onDataProcessed, numKa = $bindable(1525) } = $props<{
		onCancel: () => void;
        onDataProcessed?: (operatorData: OperatorData, ppiAssignments: PpiAssignment[]) => void;
        numKa?: number;
	}>();

    let isLoading = $state(false);

    let scheduleStatus = $state<ScheduleStatus>('main');
    let selectedDate = $state('');
    let selectedTime = $state('');

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
            console.log('ИД06:', operatorData);

            if (operatorData) {
                // Есть данные - запрашиваем ППИ
                await processAllRecords();
            } else {
                // Нет данных - сразу создаём пустую ПРЦА
                console.log('Нет данных ИД06, создаём пустую ПРЦА');
                
                // Создаём пустой объект OperatorData
                const emptyOperatorData: OperatorData = {
                    main: {
                        id: 0,
                        n_ka: numKa,
                        d_np: selectedDate,
                        data_zap: new Date().toISOString(),
                        rnf: 0,
                        n_sp: 0,
                        dsf: '',
                        k_zajv: 5,
                        n_form_id: 0,
                        used: 0
                    },
                    kvd_list: [],
                    tnp_list: [],
                    ts_list: [],
                    ona_list: [],
                    total_intervals: 0
                };
                
                // Передаём пустые данные
                onDataProcessed?.(emptyOperatorData, []);
                resetFormState();
            }
        } catch (error) {
            console.error('Ошибка при загрузке данных:', error);
            alert('Ошибка при загрузке данных: ' + (error as Error).message);
        } finally {
            isLoading = false;
        }
    }

    async function processAllRecords() {
        if (!operatorData) return;
        
        if (operatorData.kvd_list && operatorData.kvd_list.length > 0) {
            await processRecordBatch(operatorData.kvd_list, 'kvd', 'Калибровка ВД');
        }
        
        if (operatorData.tnp_list && operatorData.tnp_list.length > 0) {
            await processRecordBatch(operatorData.tnp_list, 'tnp', 'Режим ТНП');
        }
        
        if (operatorData.ts_list && operatorData.ts_list.length > 0) {
            await processRecordBatch(operatorData.ts_list, 'ts', 'Технологическая съемка');
        }

        if (operatorData.ona_list && operatorData.ona_list.length > 0) {
            await processRecordBatch(operatorData.ona_list, 'ona', 'Юстировка ОНА');
        }
        
        completePpiSelection();
    }

    function completePpiSelection() {
        if (operatorData && ppiAssignments.length > 0 && onDataProcessed) {
            if (operatorData.main) {
                operatorData.main.n_ka = numKa;
            }
            onDataProcessed(operatorData, ppiAssignments);
        }
        
        resetFormState();
    }
    
    async function processRecordBatch(
        records: any[],
        recordType: 'kvd' | 'tnp' | 'ts' | 'ona',
        recordTitle: string
    ) {
        for (let i = 0; i < records.length; i++) {
            const record = records[i];
            
            const existingAssignment = ppiAssignments.find(a => 
                a.recordId === record.id && a.recordType === recordType
            );
            
            if (existingAssignment) {
                continue;
            }
            
            await showPpiModal(record, recordType, i, records.length, recordTitle);
        }
    }
    
    async function showPpiModal(
        record: any,
        recordType: 'kvd' | 'tnp' | 'ts' | 'ona',
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
                    records = operatorData.kvd_list || [];
                    break;
                case 'tnp':
                    records = operatorData.tnp_list || [];
                    break;
                case 'ts':
                    records = operatorData.ts_list || [];
                    break;
                case 'ona':
                    records = operatorData.ona_list || [];
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
        recordType: 'kvd' | 'tnp' | 'ts' | 'ona',
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
        scheduleStatus = 'main';
        operatorData = null;
        ppiAssignments = [];
        ppiModal.isOpen = false;
        isLoading = false;
        
        onCancelProp?.();
    }

    function resetFormState() {
        scheduleStatus = 'main';
        ppiModal.isOpen = false;
        isLoading = false;
    }
</script>

<div class="creation-form-container">
    <CreationForm
        bind:scheduleStatus
        bind:selectedDate
        bind:selectedTime
        bind:numKa
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
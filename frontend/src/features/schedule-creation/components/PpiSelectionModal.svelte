<script lang="ts">
    import type {
    	Id06KvdDto,
    	Id06OnaDto,
    	Id06TnpDto,
    	Id06TsDto,
    	Ppi,
    	PpiSelectionModal as PpiModalType
    } from '$lib/types';
    import { ScheduleCreationService } from '../../services/scheduleCreation.service';

    let {
        modalData,
        ppiList = [],
        closeModal,
        applyPpi
    } = $props<{
        modalData: PpiModalType;
        ppiList?: Ppi[];
        closeModal: () => void;
        applyPpi: (
            record: Id06KvdDto | Id06TnpDto | Id06TsDto | Id06OnaDto, 
            ppi: Ppi, 
            applyToAll: boolean
        ) => void;
    }>();

    function formatDateTime(dateStr: string): string {
        return ScheduleCreationService.formatDateTime(dateStr);
    }
    
    function handleApply() {
        if (modalData.selectedPpi && modalData.currentRecord) {
            applyPpi(modalData.currentRecord, modalData.selectedPpi, false);
        }
    }

    function handleApplyToAll() {
        if (modalData.selectedPpi && modalData.currentRecord) {
            applyPpi(modalData.currentRecord, modalData.selectedPpi, true);
        }
    }
</script>

{#if modalData.isOpen}
    <div class="modal-overlay" on:click|self={closeModal}>
        <div class="modal-container">
            <div class="modal-header">
                <h2>Выбор ППИ для {modalData.recordTitle}</h2>
                <button class="close-button" on:click={closeModal}>×</button>
            </div>

            <div class="modal-content">
                <div class="record-info">
                    <div class="info-section">
                        <div class="info-row">
                            <span class="info-label">Записей:</span>
                            <span class="info-value">{modalData.recordIndex + 1} из {modalData.totalRecords}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Время начала:</span>
                            <span class="info-value">{formatDateTime(modalData.currentRecord?.dn || '')}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Время окончания:</span>
                            <span class="info-value">{formatDateTime(modalData.currentRecord?.dk || '')}</span>
                        </div>
                    </div>
                </div>

                <div class="ppi-selection">
                    <h3>Выберите ППИ (пункт приёма информации):</h3>
                    <div class="ppi-list">
                        {#each ppiList as ppi}
                            <label class="ppi-item {modalData.selectedPpi?.id === ppi.id ? 'selected' : ''}">
                                <input
                                    type="radio"
                                    name="ppi"
                                    value={ppi.id}
                                    checked={modalData.selectedPpi?.id === ppi.id}
                                    on:change={() => modalData.selectedPpi = ppi}
                                />
                                <div class="ppi-info">
                                    <span class="ppi-name">{ppi.name}</span>
                                </div>
                            </label>
                        {/each}
                    </div>
                </div>

                <div class="modal-actions">
                    <button
                        class="btn-apply"
                        on:click={handleApply}
                        disabled={!modalData.selectedPpi}
                    >
                        Применить для этой записи
                    </button>
                    <button
                        class="btn-apply-all"
                        on:click={handleApplyToAll}
                        disabled={!modalData.selectedPpi || modalData.totalRecords <= 1}
                        title={modalData.totalRecords <= 1 ? "Только одна запись этого типа" : ""}
                    >
                        Применить для всех записей типа "{modalData.recordTitle}"
                    </button>
                </div>
            </div>
        </div>
    </div>
{/if}

<style>
    .modal-overlay {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
        backdrop-filter: blur(2px);
    }

    .modal-container {
        background: white;
        border-radius: 8px;
        width: 550px;
        max-width: 90vw;
        max-height: 85vh;
        overflow: hidden;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
    }

    .modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem 1.5rem;
        background: linear-gradient(135deg, #4299e1, #3182ce);
        color: white;
    }

    .modal-header h2 {
        margin: 0;
        font-size: 1.25rem;
        font-weight: 600;
    }

    .close-button {  
        background: rgba(255, 255, 255, 0.2);
        border: none;
        color: white;
        font-size: 1.5rem;
        cursor: pointer;
        padding: 0;
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 4px;
        transition: background 0.2s;
    }

    .close-button:hover {
        background: rgba(255, 255, 255, 0.3);
    }

    .modal-content {
        padding: 1.5rem;
        overflow-y: auto;
        max-height: calc(85vh - 70px);
    }

    .record-info {
        background: #f8fafc;
        border-radius: 6px;
        padding: 1rem;
        margin-bottom: 1rem;
        border: 1px solid #e2e8f0;
    }

    .info-section {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 0.75rem;
    }

    .info-row {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
    }

    .info-label {
        font-size: 0.8rem;
        color: #718096;
        font-weight: 500;
        text-transform: uppercase;
        letter-spacing: 0.5px;
    }

    .info-value {
        font-size: 1rem;
        color: #2d3748;
        font-weight: 600;
    }

    .ppi-selection h3 {
        margin: 0 0 1rem 0;
        color: #2d3748;
        font-size: 1.1rem;
        font-weight: 600;
    }

    .ppi-list {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 0.75rem;
    }

    .ppi-item {
        display: flex;
        align-items: center;
        padding: 0.5rem 1rem;
        border: 2px solid #cbd5e0;
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.2s;
        background: white;
    }

    .ppi-item:hover {
        border-color: #4299e1;
        background: #f7fafc;
    }

    .ppi-item.selected {
        border-color: #4299e1;
        background: #ebf8ff;
        box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1);
    }

    .ppi-item input[type="radio"] {
        margin-right: 0.75rem;
        width: 18px;
        height: 18px;
    }

    .ppi-info {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
    }

    .ppi-name {
        font-weight: 600;
        color: #2d3748;
        font-size: 0.95rem;
    }

    .modal-actions {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
        padding-top: 1.5rem;
    }

    .modal-actions button {
        padding: 0.875rem;
        border: none;
        border-radius: 6px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        font-size: 0.95rem;
        text-align: center;
    }

    .btn-apply {
        background: linear-gradient(135deg, #48bb78, #38a169);
        color: white;
    }

    .btn-apply:hover:not(:disabled) {
        background: linear-gradient(135deg, #38a169, #2f855a);
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(72, 187, 120, 0.3);
    }

    .btn-apply-all {
        background: linear-gradient(135deg, #4299e1, #3182ce);
        color: white;
    }

    .btn-apply-all:hover:not(:disabled) {
        background: linear-gradient(135deg, #3182ce, #2b6cb0);
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(66, 153, 225, 0.3);
    }

    button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
        transform: none !important;
        box-shadow: none !important;
    }
</style>
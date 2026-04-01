<script lang="ts">
    let {
        isOpen = false,
        title = '',
        message = '',
        type = 'info',
        showConfirm = false,
        confirmText = 'OK',
        cancelText = 'Отмена',
        onConfirm = () => {},
        onCancel = () => {},
        onClose = () => {}
    } = $props<{
        isOpen?: boolean;
        title?: string;
        message?: string;
        type?: 'success' | 'error' | 'warning' | 'info';
        showConfirm?: boolean;
        confirmText?: string;        cancelText?: string;
        onConfirm?: () => void;
        onCancel?: () => void;
        onClose?: () => void;
    }>();

    console.log('Modal render:', { isOpen, title, message, type });

    let isVisible = $state(false);

    $effect(() => {
        if (isOpen) {
            isVisible = true;
        } else {
            isVisible = false;
        }
    });

    function handleConfirm() {
        onConfirm();
        onClose();
    }

    function handleCancel() {
        onCancel();
        onClose();
    }

    function getHeaderColor() {
        switch (type) {
            case 'success': return '#48bb78';
            case 'error': return '#f56565';
            case 'warning': return '#ed8936';
            default: return '#4299e1';
        }
    }

    function getIcon() {
        switch (type) {
            case 'success':
                return `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>`;
            case 'error':
                return `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="15" y1="9" x2="9" y2="15"></line>
                    <line x1="9" y1="9" x2="15" y2="15"></line>
                </svg>`;
            case 'warning':
                return `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 9v4"></path>
                    <path d="M12 17h.01"></path>
                    <path d="M12 3a9 9 0 1 0 9 9 9 9 0 0 0-9-9z"></path>
                </svg>`;
            default:
                return `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>`;
        }
    }
</script>

{#if isVisible}
    <div class="modal-overlay" on:click|self={onClose}>
        <div class="modal-container {isOpen ? 'open' : 'close'}">
            <div class="modal-header" style="background: {getHeaderColor()}">
                <div class="modal-header-icon">
                    {@html getIcon()}
                </div>
                <h3>{title}</h3>
            </div>
            <div class="modal-content">
                <p>{message}</p>
            </div>
            <div class="modal-actions">
                {#if showConfirm}
                    <button class="btn-confirm" on:click={handleConfirm}>
                        {confirmText}
                    </button>
                    <button class="btn-cancel" on:click={handleCancel}>
                        {cancelText}
                    </button>
                {:else}
                    <button class="btn-ok" on:click={handleConfirm}>
                        OK
                    </button>
                {/if}
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
        z-index: 1110;
        backdrop-filter: blur(2px);
    }

    .modal-container {
        background: white;
        border-radius: 12px;
        width: 450px;
        max-width: 90vw;
        overflow: hidden;
        box-shadow: 0 20px 35px rgba(0, 0, 0, 0.2);
    }

    .modal-header {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 1rem 1.5rem;
        color: white;
    }

    .modal-header-icon {
        width: 28px;
        height: 28px;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .modal-header-icon :global(.icon) {
        width: 24px;
        height: 24px;
        stroke: white;
        stroke-width: 2;
    }

    .modal-header h3 {
        margin: 0;
        font-size: 1.2rem;
        font-weight: 600;
    }

    .modal-content {
        padding: 1.5rem;
    }

    .modal-content p {
        margin: 0;
        font-size: 1rem;
        line-height: 1.5;
        color: #2d3748;
        white-space: pre-wrap;
    }

    .modal-actions {
        display: flex;
        gap: 0.75rem;
        padding: 1rem 1.5rem 1.5rem;
        justify-content: flex-end;
        border-top: 1px solid #e2e8f0;
    }

    .btn-ok,
    .btn-confirm {
        background: linear-gradient(135deg, #48bb78, #38a169);
        color: white;
        border: none;
        padding: 0.625rem 1.5rem;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 600;
        font-size: 0.9rem;
        transition: transform 0.2s, box-shadow 0.2s;
    }

    .btn-ok:hover,
    .btn-confirm:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(72, 187, 120, 0.3);
    }

    .btn-cancel {
        background: #e2e8f0;
        color: #4a5568;
        border: none;
        padding: 0.625rem 1.5rem;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 600;
        font-size: 0.9rem;
        transition: background 0.2s;
    }

    .btn-cancel:hover {
        background: #cbd5e0;
    }
</style>
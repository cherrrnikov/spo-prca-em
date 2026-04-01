import { writable } from 'svelte/store';

export interface ModalState {
    isOpen: boolean;
    title: string;
    message: string;
    type: 'success' | 'error' | 'warning' | 'info';
    showConfirm: boolean;
    confirmText: string;
    cancelText: string;
    onConfirm?: () => void;
    onCancel?: () => void;
}

function createModalStore() {
    const { subscribe, set, update } = writable<ModalState>({
        isOpen: false,
        title: '',
        message: '',
        type: 'info',
        showConfirm: false,
        confirmText: 'OK',
        cancelText: 'Отмена'
    });

    return {
        subscribe,
        alert: (title: string, message: string, type: ModalState['type'] = 'info') => {
            set({
                isOpen: true,
                title,
                message,
                type,
                showConfirm: false,
                confirmText: 'OK',
                cancelText: 'Отмена'
            });
        },
        confirm: (
            title: string,
            message: string,
            onConfirm: () => void,
            onCancel?: () => void,
            type: ModalState['type'] = 'warning'
        ) => {
            set({
                isOpen: true,
                title,
                message,
                type,
                showConfirm: true,
                confirmText: 'OK',
                cancelText: 'Отмена',
                onConfirm,
                onCancel
            });
        },
        close: () => {
            set({
                isOpen: false,
                title: '',
                message: '',
                type: 'info',
                showConfirm: false,
                confirmText: 'OK',
                cancelText: 'Отмена'
            });
        }
    };
}

export const modal = createModalStore();
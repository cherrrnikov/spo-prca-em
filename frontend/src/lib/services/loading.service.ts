import { writable } from 'svelte/store';

function createLoadingStore() {
    const { subscribe, set } = writable(false);
    let count = 0;

    return {
        subscribe,
        start: () => {
            count++;
            set(true);
        },
        stop: () => {
            count = Math.max(0, count - 1);
            if (count === 0) set(false);
        }
    };
}

export const loading = createLoadingStore();
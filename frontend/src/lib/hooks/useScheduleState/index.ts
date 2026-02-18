import { createActions } from './actions';
import { createCreators } from './creators';
import { createStores } from './stores';
import { createValidation } from './validation';

export function useScheduleState() {
    const stores = createStores();
    const creators = createCreators(stores);
    const validation = createValidation(stores);
    const actions = createActions(stores, creators, validation);

    return {
        ...stores,
        ...actions
    };
}
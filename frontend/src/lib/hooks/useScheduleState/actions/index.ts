import type { createCreators } from '../creators';
import type { createStores } from '../stores';
import type { createValidation } from '../validation';
import { createAnalysisActions } from './analysis';
import { createDataLoaders } from './dataLoaders';
import { createIntervalHandlers } from './intervalHandlers';
import { createModeHandlers } from './modeHandlers';
import { createUiHelpers } from './uiHelpers';

export function createActions(
    stores: ReturnType<typeof createStores>,
    creators: ReturnType<typeof createCreators>,
    validation: ReturnType<typeof createValidation>
) {
    const dataLoaders = createDataLoaders(stores, validation);
    const intervalHandlers = createIntervalHandlers(stores, creators, validation);
    const modeHandlers = createModeHandlers(stores, creators, validation);
    const analysisActions = createAnalysisActions(stores, validation);
    const uiHelpers = createUiHelpers(stores);

    return {
        ...dataLoaders,
        ...intervalHandlers,
        ...modeHandlers,
        ...analysisActions,
        ...uiHelpers
    };
}
import { DEFAULT_NUM_KA } from '$lib/constants/schedule';
import type {
    CreatedProgramData,
    ForecastData,
    Id02Dto,
    OperatorData,
    PpiAssignment,
    RotationInterval,
    ShadowInterval,
    TimeInterval,
    VkiInterval,
    ZasvetkaInterval
} from '$lib/types';
import type { AnalysisModalState, ProgramsListItem } from '$lib/types/analysis';
import type { UserResponse } from '$lib/types/auth';
import { derived, writable } from 'svelte/store';

export function createStores() {
    // Данные пользователя
    const userData = writable<UserResponse | null>(null);
    const isEditing = writable(true);

    // Режим создания
    const creationMode = writable<'operator' | 'reference' | null>(null);

    // Данные с бэкенда
    const operatorData = writable<OperatorData | null>(null);
    const bortData = writable<Id02Dto | null>(null);
    const forecastData = writable<ForecastData | null>(null);
    
    // Астрокоррекция
    const hasAstrocorrectionData = writable<boolean>(false);
    const vkiIntervals = writable<VkiInterval[]>([]);
    const rotationIntervals = writable<RotationInterval[]>([]);

    // Тени и засветки
    const shadowIntervals = writable<ShadowInterval[]>([]);
    const zasvetkaIntervals = writable<ZasvetkaInterval[]>([]);

    // Основные интервалы
    const intervals = writable<TimeInterval[]>([]);
    const createdPrograms = writable<CreatedProgramData[]>([]);
    const ppiAssignments = writable<PpiAssignment[]>([]);

    // Состояние загрузки
    const operatorDataLoaded = writable(false);
    const forecastDataLoaded = writable(false);

    // Дата
    const selectedProgramDate = writable<string>('');
    const contextDate = writable<string>('');

    // Редактирование
    const selectedMode = writable<number | null>(null);
    const editingInterval = writable<TimeInterval | null>(null);
    const selectedIntervalId = writable<string | null>(null);
    const selectedIntervalIds = writable<Set<string>>(new Set());

    // Анализ
    const programsList = writable<ProgramsListItem[]>([]);
    const activeProgramId = writable<string | null>(null);
    const isAnalysisMode = writable<boolean>(false);

    // Номер КА
    const numKa = writable<number>(DEFAULT_NUM_KA);

    // Номер РП
    const currentNumRp = writable<number | null>(null);

    const isReadOnly = writable(false);

    const activeProgramDate = derived(
        [programsList, activeProgramId],
        ([$programsList, $activeProgramId]) => {
            if (!$activeProgramId) return '';
            const program = $programsList.find(p => p.id === $activeProgramId);
            return program?.date || '';
        }
    );

    const analysisModal = writable<AnalysisModalState>({
        isOpen: false,
        startDate: '',
        endDate: '',
        isLoading: false
    })

    return {
        // Данные пользователя
        userData,
        isEditing,
        
        // Режим создания
        creationMode,
        
        // Данные с бэкенда
        operatorData,
        bortData,
        forecastData,
        
        // Астрокоррекция
        hasAstrocorrectionData,
        vkiIntervals,
        rotationIntervals,
        
        // Тени и засветки
        shadowIntervals,
        zasvetkaIntervals,
        
        // Основные интервалы
        intervals,
        createdPrograms,
        ppiAssignments,
        
        // Состояние загрузки
        operatorDataLoaded,
        forecastDataLoaded,
        
        // Дата
        selectedProgramDate,
        contextDate,
        
        // Редактирование
        selectedMode,
        editingInterval,
        selectedIntervalId,
        selectedIntervalIds,

        // Анализ 
        programsList,
        activeProgramId,
        isAnalysisMode,
        analysisModal,
        activeProgramDate,

        // Номер КА
        numKa,

        // Номер РП
        currentNumRp,

        isReadOnly
    };
}
import { DEFAULT_MODE_DURATION } from '$lib/config/schedule.config';
import { CUSTOMER_CODES, MODE_CODES } from '$lib/constants/schedule';
import { MsuMapper } from '$lib/mappers/msuMapper';
import { modal } from '$lib/services/modal.service';
import type { CreatedProgramData, ModeCreationForm, ProgramModeData, TimeInterval } from '$lib/types';
import { checkIntervalOverlap } from '$lib/utils/interval/conflicts';
import { IntervalValidationService } from '$lib/utils/intervalValidation';
import { ModeUtils } from '$lib/utils/mode';
import { TimeUtils } from '$lib/utils/time';
import { get } from 'svelte/store';
import { ScheduleConverterService } from '../../../../features/services/data/scheduleConverter.service';
import type { createCreators } from '../creators';
import type { createStores } from '../stores';
import type { createValidation } from '../validation';

export function createModeHandlers(
    stores: ReturnType<typeof createStores>,
    creators: ReturnType<typeof createCreators>,
    validation: ReturnType<typeof createValidation>
) {
    const {
        intervals,
        createdPrograms,
        editingInterval,
        selectedIntervalId,
        selectedIntervalIds,
        selectedMode,
        isEditing,
        operatorDataLoaded,
        creationMode,
        operatorData,      
        contextDate 
    } = stores;

    const {
        generateTempId,
        createTimeInterval,
        createProgramModeData
    } = creators;

    const {
        checkAndUpdateAllConflictsForNewInterval,
        updateAllConflicts,
        syncCurrentProgramWithStore
    } = validation;

    function handleModeSelect(modeId: number) {
        if (!get(operatorDataLoaded) || get(creationMode) !== 'operator') {
            return;
        }
        selectedMode.set(modeId);
    }

    function handleModeFormSubmit(formData: ModeCreationForm | TimeInterval[]) {
        if (!get(operatorDataLoaded) || get(creationMode) !== 'operator') {
            return;
        }
        
        // Если пришел массив интервалов (создание нескольких)
        if (Array.isArray(formData)) {
            const intervalsToAdd: TimeInterval[] = [];
            
            for (const interval of formData) {
                // Проверяем каждый интервал на пересечения
                const sameModeOverlap = checkIntervalOverlap(
                    get(intervals),
                    interval.startTime,
                    interval.dlit!,
                    interval.mode
                );
                
                if (sameModeOverlap.overlaps) {
                    modal.alert("Ошибка", `Интервал ${interval.startTime}-${interval.endTime} пересекается с существующим интервалом`, 'error')
                    return;
                }
                intervalsToAdd.push(interval);
            }
            
            // Создаем ProgramModeData для каждого интервала
            const newPrograms: CreatedProgramData[] = [];
            
            for (const interval of intervalsToAdd) {
                const tempId = generateTempId();
                const modeData = createProgramModeDataFromInterval(interval, tempId);
                
                newPrograms.push({
                    tempId,
                    modeData,
                    timeInterval: interval
                });
            }
            
            createdPrograms.update(current => [...current, ...newPrograms]);
            intervals.update(current => [...current, ...intervalsToAdd]);
            
            // Обновляем конфликты для всех новых интервалов
            intervalsToAdd.forEach(interval => {
                checkAndUpdateAllConflictsForNewInterval(interval);
            });
            updateAllConflicts();
            
        } else {
            // Старая логика для одного интервала (ModeCreationForm)
            const validation = IntervalValidationService.validateTimeInput(
                formData.startTime, 
                formData.duration
            );
            
            if (!validation.isValid) {
                modal.alert("Ошибка", `${validation.message}`, 'error')
                return;
            }

            const currentIntervals = get(intervals);
            const sameModeOverlap = checkIntervalOverlap(
                currentIntervals,
                formData.startTime,
                formData.duration,
                formData.modeType!
            );
            
            if (sameModeOverlap.overlaps) {
                modal.alert("Ошибка", `Интервал пересекается с существующим интервалом\n` +
                    `Время конфликта: ${sameModeOverlap.conflictingInterval?.startTime} - ${sameModeOverlap.conflictingInterval?.endTime}`, 'error')
                return;
            }
            
            const tempId = generateTempId();
            const endTime = TimeUtils.calculateEndTimeSeconds(formData.startTime, formData.duration);
            const modeData = createProgramModeData(formData, tempId);
            const timeInterval = createTimeInterval(formData, tempId, endTime);

            createdPrograms.update(current => [...current, { tempId, modeData, timeInterval }]);
            intervals.update(current => [...current, timeInterval]);
            checkAndUpdateAllConflictsForNewInterval(timeInterval);
            updateAllConflicts();
        }
        
        editingInterval.set(null);
        selectedIntervalId.set(null);
        isEditing.set(false);
        syncCurrentProgramWithStore();
    }

    function createProgramModeDataFromInterval(interval: TimeInterval, tempId: string): ProgramModeData {
        const currentOperatorData = get(operatorData);
        const mainId = currentOperatorData?.main?.id || 0;
        const numKa = currentOperatorData?.main?.n_ka || 1;
        
        const baseData = {
            numRp: 0,
            numKa: numKa,
            dateOn: `${interval.date}T${interval.startTime}`,
            dateOff: `${interval.date}T${interval.endTime}`,
            kodMode: interval.mode,
            numPpi: interval.ppi || 1,
            dlit: interval.dlit || DEFAULT_MODE_DURATION,
            zakazchik: ModeUtils.getCustomerLabel(CUSTOMER_CODES, interval.customerCode || 1)
        };
        
        // Для ТС (8) и обычных съемок (1)
        if (interval.mode === MODE_CODES.TS || interval.mode === MODE_CODES.SHOOTING) {
            // tsData уже должен быть заполнен при создании интервала через ManualIntervalSplitService
            // если нет — создаем на основе msuConfig
            const msuData = interval.msuData || MsuMapper.fromMsuConfigs(
                interval.msu1Config || ScheduleConverterService.getDefaultMsuConfig(),
                interval.msu2Config || ScheduleConverterService.getDefaultMsuConfig(),
                {
                    id: 0,
                    idMain: mainId,
                    tip: 1,
                    reg: 0,
                    dlit: interval.dlit || DEFAULT_MODE_DURATION,
                    prBssd: 0,
                    prZg: 0,
                    prOtklZgBssd: 0
                }
            );
            
            return {
                ...baseData,
                msuData: msuData
            };
        }
        
        // Для КВД (7)
        if (interval.mode === MODE_CODES.KVD && interval.kvdConfig) {
            return {
                ...baseData,
                kvdData: {
                    id: 0,
                    idMain: mainId,
                    prMsu: interval.kvdConfig.prMsu,
                    prBssd: interval.kvdConfig.prBssd,
                    prZg: interval.kvdConfig.prZg
                }
            };
        }
        
        // Для Юстировки ОНА (6)
        if (interval.mode === MODE_CODES.ONA) {
            return {
                ...baseData,
                onaData: {
                    id: 0,
                    idMain: mainId,
                    typeOmi: 1,
                    dN: baseData.dateOn,
                    dK: baseData.dateOff,
                    nOna: interval.nOna || 1,
                    nPpi: interval.ppi || 1
                }
            };
        }
        
        // Для ОМИ (2)
        if (interval.mode === MODE_CODES.OMI) {
            const omiData = interval.omiData || {
                id: 0,
                idMain: mainId,
                numOmi: 1,
                typeOmi: 1,
                dateNach: baseData.dateOn,
                dateCon: baseData.dateOff,
                dlit: interval.dlit || DEFAULT_MODE_DURATION
            };
            
            return {
                ...baseData,
                omiData: omiData
            };
        }
        
        // Для ТНП (4)
        if (interval.mode === MODE_CODES.TNP) {
            return {
                ...baseData,
                tnpData: {
                    id: 0,
                    idMain: mainId,
                    prMsu: 1,
                    prBssd: 1,
                    prZg: 1
                }
            };
        }
        
        return baseData;
    }

    function handleModeFormCancel() {
        editingInterval.set(null);
        selectedIntervalId.set(null);
        selectedIntervalIds.set(new Set());
        isEditing.set(false);
    }

    return {
        handleModeSelect,
        handleModeFormSubmit,
        handleModeFormCancel
    };
}
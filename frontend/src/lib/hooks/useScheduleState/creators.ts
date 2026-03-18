import { CUSTOMER_CODES } from '$lib/constants/schedule';
import type {
    ModeCreationForm,
    ProgramModeData,
    TimeInterval
} from '$lib/types';
import { ModeUtils } from '$lib/utils/mode';
import { TimeUtils } from '$lib/utils/time';
import { get } from 'svelte/store';
import { ScheduleCreationService } from '../../../features/services/scheduleCreation.service';

export function createCreators(stores: ReturnType<typeof import('./stores').createStores>) {
    const {
        contextDate,
        operatorData
    } = stores;

    function generateTempId(): string {
        return `created_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    }

    function getDefaultMsuConfig() {
        return ScheduleCreationService.getDefaultMsuConfig();
    }

    function getCustomerLabel(code: number): string {
        const customer = CUSTOMER_CODES.find(c => c.value === code);
        return customer?.label.split(' - ')[1] || '';
    }

    function createTimeInterval(
        formData: ModeCreationForm, 
        tempId: string, 
        endTime: string
    ): TimeInterval {
        const currentContextDate = get(contextDate);

        const interval: TimeInterval = {
            id: tempId,
            mode: formData.modeType!,
            date: currentContextDate,
            startTime: formData.startTime,
            endTime: endTime, 
            city: ScheduleCreationService.getCityByPpi(formData.ppiNum),
            color: ScheduleCreationService.getColorByPpi(formData.ppiNum),
            title: ModeUtils.getModeTitle(formData.modeType!),
            ppi: formData.ppiNum,
            dlit: formData.duration,
            customerCode: formData.customerCode,
            hasConflict: false,
            conflictWith: [],
            nearZasvetka: false,
            zasvetkaConflict: false,
            zasvetkaDistance: 0,
            willBeSaved: true,
        };

        if (formData.modeType === 7 && formData.kvdConfig) {
            interval.kvdConfig = { ...formData.kvdConfig };
        }
        
        if (formData.modeType === 8) {
            interval.msu1Config = { ...formData.msu1Config };
            interval.msu2Config = { ...formData.msu2Config };
        }

        if (formData.modeType === 6) {
            interval.nOna = formData.nOna || 1;
        }

        return interval;
    }

    function createUpdatedInterval(
        editingInterval: TimeInterval, 
        formData: ModeCreationForm, 
        endTime: string
    ): TimeInterval {
        const currentContextDate = get(contextDate);

        const updatedInterval: TimeInterval = {
            ...editingInterval,
            date: currentContextDate,
            startTime: formData.startTime,
            endTime: endTime, 
            ppi: formData.ppiNum,
            dlit: formData.duration,
            city: ScheduleCreationService.getCityByPpi(formData.ppiNum),
            color: ScheduleCreationService.getColorByPpi(formData.ppiNum),
            customerCode: formData.customerCode,
        };
        
        if (formData.modeType === 7 && formData.kvdConfig) {
            updatedInterval.kvdConfig = { ...formData.kvdConfig };
        }
        
        if (formData.modeType === 8) {
            updatedInterval.msu1Config = { ...formData.msu1Config };
            updatedInterval.msu2Config = { ...formData.msu2Config };

            updatedInterval.tsData = {
                id: editingInterval.tsData?.id ?? 0,
                idMain: editingInterval.tsData?.idMain ?? 0,
                tip: editingInterval.tsData?.tip ?? 1,
                // reg: editingInterval.tsData?.reg ?? 1,
                dlit: editingInterval.tsData?.dlit ?? formData.duration,
                prMsu1: editingInterval.tsData?.prMsu1 ?? 0,
                vd1Msu1: editingInterval.tsData?.vd1Msu1 ?? 0,
                vd2Msu1: editingInterval.tsData?.vd2Msu1 ?? 0,
                vd3Msu1: editingInterval.tsData?.vd3Msu1 ?? 0,
                ik4Msu1: editingInterval.tsData?.ik4Msu1 ?? 0,
                ik5Msu1: editingInterval.tsData?.ik5Msu1 ?? 0,
                ik6Msu1: editingInterval.tsData?.ik6Msu1 ?? 0,
                ik7Msu1: editingInterval.tsData?.ik7Msu1 ?? 0,
                ik8Msu1: editingInterval.tsData?.ik8Msu1 ?? 0,
                ik9Msu1: editingInterval.tsData?.ik9Msu1 ?? 0,
                ik10Msu1: editingInterval.tsData?.ik10Msu1 ?? 0,
                prMsu2: editingInterval.tsData?.prMsu2 ?? 0,
                vd1Msu2: editingInterval.tsData?.vd1Msu2 ?? 0,
                vd2Msu2: editingInterval.tsData?.vd2Msu2 ?? 0,
                vd3Msu2: editingInterval.tsData?.vd3Msu2 ?? 0,
                ik4Msu2: editingInterval.tsData?.ik4Msu2 ?? 0,
                ik5Msu2: editingInterval.tsData?.ik5Msu2 ?? 0,
                ik6Msu2: editingInterval.tsData?.ik6Msu2 ?? 0,
                ik7Msu2: editingInterval.tsData?.ik7Msu2 ?? 0,
                ik8Msu2: editingInterval.tsData?.ik8Msu2 ?? 0,
                ik9Msu2: editingInterval.tsData?.ik9Msu2 ?? 0,
                ik10Msu2: editingInterval.tsData?.ik10Msu2 ?? 0,
                
                prBssd: formData.prBssd ?? 0,
                prZg: formData.prZg ?? 0,
                prOtklZgBssd: formData.prOtklZg ?? 0,
                reg: formData.reg ?? 0
            };
        }

        if (formData.modeType === 6) {
            updatedInterval.nOna = formData.nOna || editingInterval.nOna || 1;
        }

        return updatedInterval;
    }

    function createProgramModeData(formData: ModeCreationForm, tempId: string): ProgramModeData {
        const currentOperatorData = get(operatorData);
        const currentContextDate = get(contextDate);
        const mainId = currentOperatorData?.main.id || 0;
        const endDisplayTime = TimeUtils.calculateEndTimeSeconds(formData.startTime, formData.duration);
        const dateOff = `${currentContextDate}T${endDisplayTime}`;
        const dateOn = `${currentContextDate}T${formData.startTime}`; 

        const baseData = {
            numRp: 0,
            numKa: currentOperatorData?.main.n_ka || 1,
            dateOn: dateOn,
            dateOff: dateOff,
            kodMode: formData.modeType!,
            numPpi: formData.ppiNum,
            dlit: formData.duration,
            zakazchik: getCustomerLabel(formData.customerCode)
        };
        
        if (formData.modeType === 7) {
            const kvdConfig = formData.kvdConfig || {
                prMsu: 0,
                prBssd: 0,
                prZg: 0
            };
            
            return {
                ...baseData,
                kvdData: {
                    id: 0,
                    idMain: mainId,
                    prMsu: kvdConfig.prMsu,
                    prBssd: kvdConfig.prBssd,
                    prZg: kvdConfig.prZg
                }
            };
        } else if (formData.modeType === 8) {
            const msu1Config = formData.msu1Config || getDefaultMsuConfig();
            const msu2Config = formData.msu2Config || getDefaultMsuConfig();
            const tip = (formData as any).tip || 1;  
            const reg = (formData as any).reg || 1;

            return {
                ...baseData,
                tsData: {
                    id: 0,
                    idMain: mainId,
                    tip: tip,
                    reg: reg,
                    dlit: baseData.dlit,
                    prMsu1: msu1Config.prMsu || 0,
                    vd1Msu1: msu1Config.vd1 || 0,
                    vd2Msu1: msu1Config.vd2 || 0,
                    vd3Msu1: msu1Config.vd3 || 0,
                    ik4Msu1: msu1Config.ik4 || 0,
                    ik5Msu1: msu1Config.ik5 || 0,
                    ik6Msu1: msu1Config.ik6 || 0,
                    ik7Msu1: msu1Config.ik7 || 0,
                    ik8Msu1: msu1Config.ik8 || 0,
                    ik9Msu1: msu1Config.ik9 || 0,
                    ik10Msu1: msu1Config.ik10 || 0,
                    prMsu2: msu2Config.prMsu || 0,
                    vd1Msu2: msu2Config.vd1 || 0,
                    vd2Msu2: msu2Config.vd2 || 0,
                    vd3Msu2: msu2Config.vd3 || 0,
                    ik4Msu2: msu2Config.ik4 || 0,
                    ik5Msu2: msu2Config.ik5 || 0,
                    ik6Msu2: msu2Config.ik6 || 0,
                    ik7Msu2: msu2Config.ik7 || 0,
                    ik8Msu2: msu2Config.ik8 || 0,
                    ik9Msu2: msu2Config.ik9 || 0,
                    ik10Msu2: msu2Config.ik10 || 0,
                    prBssd: 0,
                    prZg: 0,
                    prOtklZgBssd: 0
                }
            };
        } else if (formData.modeType === 4) {
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
        } else if (formData.modeType === 6) {
            return {
                ...baseData,
                onaData: {
                    id: 0,
                    idMain: mainId,
                    typeOmi: 1,
                    dN: dateOn,
                    dK: dateOff,
                    nOna: formData.nOna || 1,
                    nPpi: formData.ppiNum
                }
            };
        } else {
            return baseData;
        }
    }

    return {
        generateTempId,
        getDefaultMsuConfig,
        createTimeInterval,
        createUpdatedInterval,
        createProgramModeData
    };
}
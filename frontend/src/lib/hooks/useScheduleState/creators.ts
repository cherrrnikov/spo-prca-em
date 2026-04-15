import { CUSTOMER_CODES } from '$lib/constants/schedule';
import type {
    ModeCreationForm,
    ProgramModeData,
    TimeInterval
} from '$lib/types';
import { getDefaultIntervalFlags } from '$lib/utils/interval';
import { ModeUtils } from '$lib/utils/mode';
import { TimeUtils } from '$lib/utils/time';
import { get } from 'svelte/store';
import { ScheduleConverterService } from '../../../features/services/data/scheduleConverter.service';
import { CityService } from '../../../features/services/utils/cities.service';

export function createCreators(stores: ReturnType<typeof import('./stores').createStores>) {
    const {
        contextDate,
        operatorData,
        bortData
    } = stores;

    function generateTempId(): string {
        return `created_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
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
        const currentBortData = get(bortData);

        const interval: TimeInterval = {
            id: tempId,
            mode: formData.modeType!,
            date: currentContextDate,
            startTime: formData.startTime,
            endTime: endTime, 
            city: CityService.getCityByPpi(formData.ppiNum),
            color: CityService.getColorByPpi(formData.ppiNum),
            title: ModeUtils.getModeTitle(formData.modeType!),
            ppi: formData.ppiNum,
            dlit: formData.duration,
            customerCode: formData.customerCode,
            ...getDefaultIntervalFlags()
        };

        if (formData.modeType === 7 && formData.kvdConfig) {
            interval.kvdConfig = { ...formData.kvdConfig };
        }
        
        if (formData.modeType === 8 || formData.modeType === 1) {
            interval.msu1Config = { ...formData.msu1Config };
            interval.msu2Config = { ...formData.msu2Config };

            if (formData.modeType === 8) {
                // ТС - берем ВСЕ из формы (данные из ИД06)
                interval.tsData = {
                    id: 0,
                    idMain: 0,
                    tip: formData.tip ?? 1,
                    reg: formData.reg ?? 0,
                    dlit: formData.duration,
                    prMsu1: formData.msu1Config.prMsu || 0,
                    vd1Msu1: formData.msu1Config.vd1 || 0,
                    vd2Msu1: formData.msu1Config.vd2 || 0,
                    vd3Msu1: formData.msu1Config.vd3 || 0,
                    ik4Msu1: formData.msu1Config.ik4 || 0,
                    ik5Msu1: formData.msu1Config.ik5 || 0,
                    ik6Msu1: formData.msu1Config.ik6 || 0,
                    ik7Msu1: formData.msu1Config.ik7 || 0,
                    ik8Msu1: formData.msu1Config.ik8 || 0,
                    ik9Msu1: formData.msu1Config.ik9 || 0,
                    ik10Msu1: formData.msu1Config.ik10 || 0,
                    prMsu2: formData.msu2Config.prMsu || 0,
                    vd1Msu2: formData.msu2Config.vd1 || 0,
                    vd2Msu2: formData.msu2Config.vd2 || 0,
                    vd3Msu2: formData.msu2Config.vd3 || 0,
                    ik4Msu2: formData.msu2Config.ik4 || 0,
                    ik5Msu2: formData.msu2Config.ik5 || 0,
                    ik6Msu2: formData.msu2Config.ik6 || 0,
                    ik7Msu2: formData.msu2Config.ik7 || 0,
                    ik8Msu2: formData.msu2Config.ik8 || 0,
                    ik9Msu2: formData.msu2Config.ik9 || 0,
                    ik10Msu2: formData.msu2Config.ik10 || 0,
                    // Для ТС - берем из формы (это данные из ИД06)
                    prBssd: currentBortData?.pr_bssd ?? 0,
                    prZg: currentBortData?.pr_zg ?? 0,
                    prOtklZgBssd: formData.prOtklZg ?? 0
                };
            } else {
                // Обычная съемка (mode 1) - берем prBssd и prZg из ИД02
                interval.tsData = {
                    id: 0,
                    idMain: 0,
                    tip: formData.tip ?? 1,
                    reg: formData.reg ?? 0,
                    dlit: formData.duration,
                    prMsu1: formData.msu1Config.prMsu || 0,
                    vd1Msu1: formData.msu1Config.vd1 || 0,
                    vd2Msu1: formData.msu1Config.vd2 || 0,
                    vd3Msu1: formData.msu1Config.vd3 || 0,
                    ik4Msu1: formData.msu1Config.ik4 || 0,
                    ik5Msu1: formData.msu1Config.ik5 || 0,
                    ik6Msu1: formData.msu1Config.ik6 || 0,
                    ik7Msu1: formData.msu1Config.ik7 || 0,
                    ik8Msu1: formData.msu1Config.ik8 || 0,
                    ik9Msu1: formData.msu1Config.ik9 || 0,
                    ik10Msu1: formData.msu1Config.ik10 || 0,
                    prMsu2: formData.msu2Config.prMsu || 0,
                    vd1Msu2: formData.msu2Config.vd1 || 0,
                    vd2Msu2: formData.msu2Config.vd2 || 0,
                    vd3Msu2: formData.msu2Config.vd3 || 0,
                    ik4Msu2: formData.msu2Config.ik4 || 0,
                    ik5Msu2: formData.msu2Config.ik5 || 0,
                    ik6Msu2: formData.msu2Config.ik6 || 0,
                    ik7Msu2: formData.msu2Config.ik7 || 0,
                    ik8Msu2: formData.msu2Config.ik8 || 0,
                    ik9Msu2: formData.msu2Config.ik9 || 0,
                    ik10Msu2: formData.msu2Config.ik10 || 0,
                    // Для съемки - берем из ИД02
                    prBssd: currentBortData?.pr_bssd ?? 0,
                    prZg: currentBortData?.pr_zg ?? 0,
                    prOtklZgBssd: currentBortData?.pr_otkl_zg ?? 0
                };
            }
        }

        if (formData.modeType === 6) {
            interval.nOna = formData.nOna || 1;
        }

        if (formData.modeType === 2) {
            interval.omiData = {
                id: 0,
                idMain: 0,
                numOmi: 1,
                typeOmi: formData.typeOmi ?? 1,
                dateNach: `${currentContextDate}T${formData.startTime}`,
                dateCon: `${currentContextDate}T${endTime}`,
                dlit: formData.duration
            };
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
            city: CityService.getCityByPpi(formData.ppiNum),
            color: CityService.getColorByPpi(formData.ppiNum),
            customerCode: formData.customerCode,
        };
        
        if (formData.modeType === 7 && formData.kvdConfig) {
            updatedInterval.kvdConfig = { ...formData.kvdConfig };
        }
        
        if (formData.modeType === 8 || formData.modeType === 1) {
            updatedInterval.msu1Config = { ...formData.msu1Config };
            updatedInterval.msu2Config = { ...formData.msu2Config };

            updatedInterval.tsData = {
                id: editingInterval.tsData?.id ?? 0,
                idMain: editingInterval.tsData?.idMain ?? 0,
                
                // ВСЕ редактируемые поля берем ИЗ ФОРМЫ
                tip: formData.tip ?? 1,
                reg: formData.reg ?? 0,
                dlit: formData.duration,
                
                // Настройки МСУ - из formData.msu1Config/msu2Config
                prMsu1: formData.msu1Config.prMsu || 0,
                vd1Msu1: formData.msu1Config.vd1 || 0,
                vd2Msu1: formData.msu1Config.vd2 || 0,
                vd3Msu1: formData.msu1Config.vd3 || 0,
                ik4Msu1: formData.msu1Config.ik4 || 0,
                ik5Msu1: formData.msu1Config.ik5 || 0,
                ik6Msu1: formData.msu1Config.ik6 || 0,
                ik7Msu1: formData.msu1Config.ik7 || 0,
                ik8Msu1: formData.msu1Config.ik8 || 0,
                ik9Msu1: formData.msu1Config.ik9 || 0,
                ik10Msu1: formData.msu1Config.ik10 || 0,
                prMsu2: formData.msu2Config.prMsu || 0,
                vd1Msu2: formData.msu2Config.vd1 || 0,
                vd2Msu2: formData.msu2Config.vd2 || 0,
                vd3Msu2: formData.msu2Config.vd3 || 0,
                ik4Msu2: formData.msu2Config.ik4 || 0,
                ik5Msu2: formData.msu2Config.ik5 || 0,
                ik6Msu2: formData.msu2Config.ik6 || 0,
                ik7Msu2: formData.msu2Config.ik7 || 0,
                ik8Msu2: formData.msu2Config.ik8 || 0,
                ik9Msu2: formData.msu2Config.ik9 || 0,
                ik10Msu2: formData.msu2Config.ik10 || 0,
                
                // Параметры БССД/ЗГ из формы
                prBssd: formData.prBssd ?? 0,
                prZg: formData.prZg ?? 0,
                prOtklZgBssd: formData.prOtklZg ?? 0
            };
        }

        if (formData.modeType === 6) {
            updatedInterval.nOna = formData.nOna || editingInterval.nOna || 1;
        }

        if (formData.modeType === 2) {
            updatedInterval.omiData = {
                id: editingInterval.omiData?.id ?? 0,
                idMain: editingInterval.omiData?.idMain ?? 0,
                numOmi: editingInterval.omiData?.numOmi ?? 1,
                typeOmi: formData.typeOmi ?? 1,
                dateNach: editingInterval.omiData?.dateNach ?? `${currentContextDate}T${formData.startTime}`,
                dateCon: editingInterval.omiData?.dateCon ?? `${currentContextDate}T${endTime}`,
                dlit: formData.duration
            };
        }

        return updatedInterval;
    }

    function createProgramModeData(formData: ModeCreationForm, tempId: string): ProgramModeData {
        const currentOperatorData = get(operatorData);
        const currentBortData = get(bortData)
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
        } else if (formData.modeType === 8 || formData.modeType === 1) {
            const msu1Config = formData.msu1Config || ScheduleConverterService.getDefaultMsuConfig();
            const msu2Config = formData.msu2Config || ScheduleConverterService.getDefaultMsuConfig();
            const tip = (formData as any).tip || 1;  
            const reg = (formData as any).reg || 1;

            const baseTsData = {
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
            };

            if (formData.modeType === 8) {
                // ТС - берем из формы (данные из ИД06)
                return {
                    ...baseData,
                    tsData: {
                        ...baseTsData,
                        prBssd: currentBortData?.pr_bssd ?? 0,
                        prZg: currentBortData?.pr_zg ?? 0,
                        prOtklZgBssd: formData.prOtklZg ?? 0
                    }
                };
            } else {
                // Обычная съемка - берем из ИД02
                return {
                    ...baseData,
                    tsData: {
                        ...baseTsData,
                        prBssd: currentBortData?.pr_bssd ?? 0,
                        prZg: currentBortData?.pr_zg ?? 0,
                        prOtklZgBssd: currentBortData?.pr_otkl_zg ?? 0
                    }
                };
            }
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
        } else if (formData.modeType === 2) {
            return {
                ...baseData,
                omiData: {
                    id: 0,
                    idMain: mainId,
                    numOmi: 1, 
                    typeOmi: formData.typeOmi ?? 1,
                    dateNach: dateOn,
                    dateCon: dateOff,
                    dlit: formData.duration
                }
            };
        } else {
            return baseData; 
        }
    }

    return {
        generateTempId,
        createTimeInterval,
        createUpdatedInterval,
        createProgramModeData
    };
}
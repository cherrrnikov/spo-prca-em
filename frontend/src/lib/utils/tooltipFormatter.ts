import { PPI_LIST } from '$lib/constants/schedule';
import type { ProgramModeData, TimeInterval } from '$lib/types';
import { TimeUtils } from '$lib/utils/time';

function getPpiDisplay(ppiNum: number): string {
    const ppi = PPI_LIST.find(p => p.numPpi === ppiNum);
    return ppi?.name || `ППИ ${ppiNum}`;
}

export class TooltipFormatter {
    // КВД (mode 7)
    static formatKvdTooltip(modeData: ProgramModeData): string {
        const lines = [
            `КАЛИБРОВКА ВД`,
            `Номер КА: ${modeData.numKa}`,
            `ППИ: ${getPpiDisplay(modeData.numPpi)}`,
            `Длительность: ${modeData.dlit} сек`,
            `Заказчик: ${modeData.zakazchik || 'не указан'}`,
            ``,
            `ПАРАМЕТРЫ КВД:`,
            `МСУ: ${modeData.kvdData?.prMsu === 0 ? 'МСУ-1' : 'МСУ-2'}`,
            `БССД: ${modeData.kvdData?.prBssd === 0 ? 'БССД1' : 'БССД2'}`,
            `ЗГ: ЗГ${(modeData.kvdData?.prZg || 0) + 1}`,
            ``,
            `Начало: ${TimeUtils.formatDateTime(modeData.dateOn)}`,
            `Конец: ${TimeUtils.formatDateTime(modeData.dateOff)}`
        ];
        return lines.join('\n');
    }

    // ТНП (mode 4)
    static formatTnpTooltip(modeData: ProgramModeData): string {
        const lines = [
            `РЕЖИМ ТНП`,
            `Номер КА: ${modeData.numKa}`,
            `ППИ: ${getPpiDisplay(modeData.numPpi)}`,
            `Длительность: ${modeData.dlit} сек`,
            `Заказчик: ${modeData.zakazchik || 'не указан'}`,
            ``,
            `ПАРАМЕТРЫ ТНП:`,
            `МСУ: ${modeData.tnpData?.prMsu === 0 ? 'МСУ-1' : 'МСУ-2'}`,
            `БССД: ${modeData.tnpData?.prBssd === 0 ? 'БССД1' : 'БССД2'}`,
            `ЗГ: ЗГ${(modeData.tnpData?.prZg || 0) + 1}`,
            ``,
            `Начало: ${TimeUtils.formatDateTime(modeData.dateOn)}`,
            `Конец: ${TimeUtils.formatDateTime(modeData.dateOff)}`
        ];
        return lines.join('\n');
    }

    // ТС (mode 8) и Обычные съемки (mode 1)
    static formatShootingTooltip(modeData: ProgramModeData, isTech: boolean = true): string {
        const title = isTech ? `ТЕХНОЛОГИЧЕСКАЯ СЪЕМКА` : `СЪЕМКА`;
        
        const lines = [
            title,
            `Номер КА: ${modeData.numKa}`,
            `ППИ: ${getPpiDisplay(modeData.numPpi)}`,
            `Длительность: ${modeData.dlit} сек`,
            `Заказчик: ${modeData.zakazchik || 'не указан'}`,
            ``,
            `ПАРАМЕТРЫ СЪЕМКИ:`,
            `Тип: ${modeData.msuData?.tip === 1 ? 'штатная' : 'учащенная'}`,
            `Режим: ${formatRegime(modeData.msuData?.reg)}`,
            ``,
            `МСУ-ГС 1: ${modeData.msuData?.prMsu1 ? 'задействован' : 'не задействован'}`,
            ...formatMsu1Channels(modeData.msuData),
            ``,
            `МСУ-ГС 2: ${modeData.msuData?.prMsu2 ? 'задействован' : 'не задействован'}`,
            ...formatMsu2Channels(modeData.msuData),
            ``,
            `БССД: ${modeData.msuData?.prBssd ? 'включен' : 'выключен'}`,
            `ЗГ: ЗГ${(modeData.msuData?.prZg || 0) + 1}`,
            `Отключение ЗГ: ${modeData.msuData?.prOtklZgBssd ? 'требуется' : 'не требуется'}`,
            ``,
            `Начало: ${TimeUtils.formatDateTime(modeData.dateOn)}`,
            `Конец: ${TimeUtils.formatDateTime(modeData.dateOff)}`
        ];
        return lines.join('\n');
    }

    // ОНА (mode 6)
    static formatOnaTooltip(modeData: ProgramModeData): string {
        const lines = [
            `ЮСТИРОВКА ОНА`,
            `Номер КА: ${modeData.numKa}`,
            `ППИ: ${getPpiDisplay(modeData.numPpi)}`,
            `Длительность: ${modeData.dlit} сек`,
            `Заказчик: ${modeData.zakazchik || 'не указан'}`,
            ``,
            `ПАРАМЕТРЫ ОНА:`,
            `Антенна: ОНА${modeData.onaData?.nOna || 1}`,
            ``,
            `Начало: ${TimeUtils.formatDateTime(modeData.dateOn)}`,
            `Конец: ${TimeUtils.formatDateTime(modeData.dateOff)}`
        ];
        return lines.join('\n');
    }

    // ОМИ (mode 2)
    static formatOmiTooltip(modeData: ProgramModeData): string {
        const lines = [
            `РАСПРОСТРАНЕНИЕ ОМИ`,
            `Номер КА: ${modeData.numKa}`,
            `ППИ: ${getPpiDisplay(modeData.numPpi)}`,
            `Длительность: ${modeData.dlit} сек`,
            `Заказчик: ${modeData.zakazchik || 'не указан'}`,
            ``,
            `ПАРАМЕТРЫ ОМИ:`,
            `Номер ОМИ: ${modeData.omiData?.numOmi || 1}`,
            `Тип ОМИ: ${modeData.omiData?.typeOmi || 1}`,
            ``,
            `Начало: ${TimeUtils.formatDateTime(modeData.dateOn)}`,
            `Конец: ${TimeUtils.formatDateTime(modeData.dateOff)}`
        ];
        return lines.join('\n');
    }

    static formatTooltip(interval: TimeInterval, modeData?: ProgramModeData): string {
        if (!modeData) {
            return interval.title || '';
        }

        switch (modeData.kodMode) {
            case 7: return this.formatKvdTooltip(modeData);
            case 4: return this.formatTnpTooltip(modeData);
            case 8: return this.formatShootingTooltip(modeData, true);  // ТС
            case 1: return this.formatShootingTooltip(modeData, false); // Обычные съемки
            case 6: return this.formatOnaTooltip(modeData);
            case 2: return this.formatOmiTooltip(modeData);
            default: return interval.title || '';
        }
    }
}

function formatRegime(reg: number | undefined): string {
    switch(reg) {
        case 0: return 'ДС (дневная съемка)';
        case 1: return 'НС (ночная съемка)';
        case 10: return 'СР1';
        case 11: return 'СР2';
        case 100: return 'СР3';
        default: return `код ${reg}`;
    }
}

function formatMsu1Channels(msuData: any): string[] {
    const lines: string[] = [];
    const vdChannels: string[] = [];
    const ikChannels: string[] = [];
    
    // Проверяем ВД каналы МСУ1
    if (msuData?.vd1Msu1 === 1) vdChannels.push('ВД1');
    if (msuData?.vd2Msu1 === 1) vdChannels.push('ВД2');
    if (msuData?.vd3Msu1 === 1) vdChannels.push('ВД3');
    
    // Проверяем ИК каналы МСУ1
    if (msuData?.ik4Msu1 === 1) ikChannels.push('ИК4');
    if (msuData?.ik5Msu1 === 1) ikChannels.push('ИК5');
    if (msuData?.ik6Msu1 === 1) ikChannels.push('ИК6');
    if (msuData?.ik7Msu1 === 1) ikChannels.push('ИК7');
    if (msuData?.ik8Msu1 === 1) ikChannels.push('ИК8');
    if (msuData?.ik9Msu1 === 1) ikChannels.push('ИК9');
    if (msuData?.ik10Msu1 === 1) ikChannels.push('ИК10');
    
    if (vdChannels.length > 0) {
        lines.push(`  ВД: ${vdChannels.join(', ')}`);
    }
    if (ikChannels.length > 0) {
        lines.push(`  ИК: ${ikChannels.join(', ')}`);
    }
    if (vdChannels.length === 0 && ikChannels.length === 0) {
        lines.push(`  Каналы: не задействованы`);
    }
    
    return lines;
}

function formatMsu2Channels(msuData: any): string[] {
    const lines: string[] = [];
    const vdChannels: string[] = [];
    const ikChannels: string[] = [];
    
    // Проверяем ВД каналы МСУ2
    if (msuData?.vd1Msu2 === 1) vdChannels.push('ВД1');
    if (msuData?.vd2Msu2 === 1) vdChannels.push('ВД2');
    if (msuData?.vd3Msu2 === 1) vdChannels.push('ВД3');
    
    // Проверяем ИК каналы МСУ2
    if (msuData?.ik4Msu2 === 1) ikChannels.push('ИК4');
    if (msuData?.ik5Msu2 === 1) ikChannels.push('ИК5');
    if (msuData?.ik6Msu2 === 1) ikChannels.push('ИК6');
    if (msuData?.ik7Msu2 === 1) ikChannels.push('ИК7');
    if (msuData?.ik8Msu2 === 1) ikChannels.push('ИК8');
    if (msuData?.ik9Msu2 === 1) ikChannels.push('ИК9');
    if (msuData?.ik10Msu2 === 1) ikChannels.push('ИК10');
    
    if (vdChannels.length > 0) {
        lines.push(`  ВД: ${vdChannels.join(', ')}`);
    }
    if (ikChannels.length > 0) {
        lines.push(`  ИК: ${ikChannels.join(', ')}`);
    }
    if (vdChannels.length === 0 && ikChannels.length === 0) {
        lines.push(`  Каналы: не задействованы`);
    }
    
    return lines;
}
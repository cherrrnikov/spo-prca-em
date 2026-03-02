import type { ProgramModeData, TimeInterval } from '$lib/types';
import { TimeUtils } from './time';

export class TooltipFormatter {
    static formatKvdTooltip(modeData: ProgramModeData): string {
        const lines = [
            `КАЛИБРОВКА ВД`,
            `Номер КА: ${modeData.numKa}`,
            `Номер ПРЦА: ${modeData.numRp}`,
            `ППИ: ${modeData.numPpi}`,
            `Длительность: ${modeData.dlit} сек`,
            `Заказчик: ${modeData.zakazchik || 'не указан'}`,
            `Комплект МСУ: ${modeData.kvdData?.prMsu === 0 ? 'МСУ-1' : 'МСУ-2'}`,
            `БССД: ${modeData.kvdData?.prBssd === 0 ? 'БССД1' : 'БССД2'}`,
            `ЗГ: ЗГ${(modeData.kvdData?.prZg || 0) + 1}`,
            `Начало: ${TimeUtils.formatDateTime(modeData.dateOn)}`,
            `Конец: ${TimeUtils.formatDateTime(modeData.dateOff)}`
        ];
        return lines.join('\n');
    }

    static formatKvdFromId06(
        kvd: any, 
        main: any, 
        ppiNum: number, 
        customerCode?: number
    ): string {
        const lines = [
            `КАЛИБРОВКА ВД (ИД06)`,
            `Номер КА: ${main?.n_ka || 1}`,
            `Номер ПРЦА: 0`,
            `ППИ: ${ppiNum}`,
            `Длительность: ${TimeUtils.calculateDuration(kvd.dn, kvd.dk)} сек`,
            `Заказчик: ${this.getCustomerLabel(customerCode || main?.k_zajv || 5)}`,
            `Комплект МСУ: ${kvd.pr_msu === 0 ? 'МСУ-1' : 'МСУ-2'}`,
            `БССД: ${kvd.pr_bssd === 0 ? 'БССД1' : 'БССД2'}`,
            `ЗГ: ЗГ${(kvd.pr_zg || 0) + 1}`,
            `Начало: ${TimeUtils.formatDateTime(kvd.dn)}`,
            `Конец: ${TimeUtils.formatDateTime(kvd.dk)}`
        ];
        return lines.join('\n');
    }

    private static getCustomerLabel(code: number): string {
        const customerLabels: Record<number, string> = {
            1: 'Заказчик 1',
            2: 'Заказчик 2', 
            3: 'Заказчик 3',
            4: 'Заказчик 4',
            5: 'Заказчик 5'
        };
        return customerLabels[code] || 'Неизвестный заказчик';
    }

    static formatTooltip(
        interval: TimeInterval, 
        modeData?: ProgramModeData,
        operatorData?: any,
        ppiNum?: number
    ): string {
        // Если есть modeData (созданный вручную или из анализа)
        if (modeData) {
            switch (modeData.kodMode) {
                case 7: return this.formatKvdTooltip(modeData);
                default: return `${interval.title || ''}`;
            }
        }
        
        // Если это интервал из ИД06
        if (operatorData && operatorData.main) {
            // Пробуем найти соответствующий КВД в operatorData
            const kvdId = interval.id.replace('kvd_', '');
            const kvd = operatorData.kvd_list?.find((k: any) => k.id.toString() === kvdId);
            
            if (kvd) {
                return this.formatKvdFromId06(
                    kvd, 
                    operatorData.main, 
                    ppiNum || 1,
                    operatorData.main?.k_zajv
                );
            }
        }
        
        return interval.title || '';
    }
}
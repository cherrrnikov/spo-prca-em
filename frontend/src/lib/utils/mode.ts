import { MODE_CODES, MODE_NAMES, WORK_MODES } from '$lib/constants/schedule';
import type { WorkMode } from '$lib/types';

export class ModeUtils {
    static getModeTitle(modeType: number): string {
        return MODE_NAMES[modeType] || this.getDefaultModeTitle(modeType);
    }

    private static getDefaultModeTitle(modeType: number): string {
        switch(modeType) {
            case MODE_CODES.ASTROCORRECTION: return 'Астрокоррекции';
            case MODE_CODES.SHOOTING: return 'Съемки';
            case MODE_CODES.OMI: return 'Распр. ОМИ';
            case MODE_CODES.TNP: return 'Режим ТНП';
            case MODE_CODES.KVD: return 'Калибровка ВД';
            case MODE_CODES.TS: return 'Технологическая съемка';
            case MODE_CODES.ONA: return 'Юстировки ОНА';
            default: return 'Режим';
        }
    }

    static getWorkModeById(modeId: number): WorkMode | undefined {
        return WORK_MODES.find(m => m.id === modeId);
    }

    static getCustomerLabel(codes: Array<{value: number, label: string}>, code: number): string {
        const customer = codes.find(c => c.value === code);
        return customer?.label.split(' - ')[1] || '';
    }
}
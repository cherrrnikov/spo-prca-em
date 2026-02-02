import { MODE_NAMES, WORK_MODES } from '$lib/constants/schedule';
import type { WorkMode } from '$lib/types/schedule';

export class ModeUtils {
    static getModeTitle(modeType: number): string {
        return MODE_NAMES[modeType] || this.getDefaultModeTitle(modeType);
    }

    private static getDefaultModeTitle(modeType: number): string {
        switch(modeType) {
            case 9: return 'Астрокоррекции';
            case 1: return 'Съемки';
            case 2: return 'Распр. ОМИ';
            case 4: return 'Режим ТНП';
            case 7: return 'Калибровка ВД';
            case 8: return 'Технологическая съемка';
            case 6: return 'Юстировки ОНА';
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
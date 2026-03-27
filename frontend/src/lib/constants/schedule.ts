import type {
    City,
    CustomerCode,
    Ppi,
    WorkMode,
    ZgOption
} from '$lib/types';

export const CUSTOMER_CODES: CustomerCode[] = [
    { value: 1, label: '01 - Заказчик 1'},
    { value: 2, label: '02 - Заказчик 2'},
    { value: 3, label: '03 - Заказчик 3'},
    { value: 4, label: '04 - Заказчик 4'},
    { value: 5, label: '05 - Заказчик 5'}
];

export const WORK_MODES: WorkMode[] = [
    { id: 9, label: 'Астрокорр.', order: '0' },
    { id: 1, label: 'Съемки', order: '1' },
    { id: 2, label: 'Распр. ОМИ', order: '2' },
    { id: 4, label: 'Режимы ТНП', order: '3' },
    { id: 7, label: 'Калибр. ВД', order: '4' },
    { id: 8, label: 'Техн. съемки', order: '5' },
    { id: 6, label: 'Юстировки ОНА', order: '6' }
];

export const MODE_ID_TO_CODE: Record<number, string> = {
    9: 'astr',  // Астрокоррекция
    1: 's',     // Съемки
    2: 'omi',   // ОМИ
    4: 'tnp',   // ТНП
    7: 'kvd',   // КВД
    8: 'ts',    // ТС
    6: 'ona'    // Юстировка ОНА
};

export const MODE_NAMES: Record<number, string> = {
    9: 'Астрокоррекция',
    1: 'Съемки',
    2: 'Распр. ОМИ',
    4: 'Режимы ТНП',
    7: 'Калибр. ВД',
    8: 'Техн. съемки',
    6: 'Юстировки ОНА'
};

export const PPI_LIST: Ppi[] = [
    { id: 1, name: '0 - Обнинск', numPpi: 1 },
    { id: 2, name: '1 - Долгопрудный', numPpi: 2 },
    { id: 3, name: '2 - Новосибирск', numPpi: 3 },
    { id: 4, name: '3 - Хабаровск', numPpi: 4 },
    { id: 5, name: '4 - Байконур', numPpi: 5 },
    { id: 6, name: '5 - Ханты-Мансийск', numPpi: 6 },
    { id: 7, name: '6 - Железногорск', numPpi: 7 },
    { id: 8, name: '7 - Улан-Удэ', numPpi: 8 },
    { id: 9, name: '8 - Москва (НЦ ОМЗ)', numPpi: 9 },
    { id: 10, name: '9 - Москва (НИЦ "Планета")', numPpi: 10 }
];

export const CITIES: City[] = [
    { id: 'obninsk', name: 'Обнинск', color: '#f4fc0a' },
    { id: 'dolgoprudniy', name: 'Долгопрудный', color: '#b80afc' },
    { id: 'novosibirsk', name: 'Новосибирск', color: '#0afcf4' },
    { id: 'khabarovsk', name: 'Хабаровск', color: '#593315' },
    { id: 'baykonur', name: 'Байконур', color: '#152359' },
    { id: 'khanty-mansiysk', name: 'Ханты-Мансийск', color: '#78866b' },
    { id: 'zheleznogorsk', name: 'Железногорск', color: '#6110b3' },
    { id: 'ulan-ude', name: 'Улан-Удэ', color: '#6197c9' },
    { id: 'moscow-omz', name: 'Москва (НЦ ОМЗ)', color: '#1a5216' },
    { id: 'moscow-planeta', name: 'Москва (ФГБУ НИЦ "Планета")', color: '#24f016' }
];

export const ZG_OPTIONS: ZgOption[] = [
    { value: 0, label: 'ЗГ1' },
    { value: 1, label: 'ЗГ2' },
    { value: 2, label: 'ЗГ3' },
    { value: 3, label: 'ЗГ4' }
];

export const DEFAULT_NUM_KA = 1525;
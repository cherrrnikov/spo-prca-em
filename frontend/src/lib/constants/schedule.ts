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

export const MODE_CODES = {
    SHOOTING: 1,
    OMI: 2,
    TNP: 4,
    ONA: 6,
    KVD: 7,
    TS: 8,
    ASTROCORRECTION: 9
} as const;

export const WORK_MODES: WorkMode[] = [
    { id: MODE_CODES.ASTROCORRECTION, label: 'Астрокорр.', order: '0' },
    { id: MODE_CODES.SHOOTING, label: 'Съемки', order: '1' },
    { id: MODE_CODES.OMI, label: 'Распр. ОМИ', order: '2' },
    { id: MODE_CODES.TNP, label: 'Режимы ТНП', order: '3' },
    { id: MODE_CODES.KVD, label: 'Калибр. ВД', order: '4' },
    { id: MODE_CODES.TS, label: 'Техн. съемки', order: '5' },
    { id: MODE_CODES.ONA, label: 'Юстировки ОНА', order: '6' }
];

export const MODE_ID_TO_CODE: Record<number, string> = {
    [MODE_CODES.ASTROCORRECTION]: 'astr',
    [MODE_CODES.SHOOTING]: 's',
    [MODE_CODES.OMI]: 'omi',
    [MODE_CODES.TNP]: 'tnp',
    [MODE_CODES.KVD]: 'kvd',
    [MODE_CODES.TS]: 'ts',
    [MODE_CODES.ONA]: 'ona'
};

export const MODE_NAMES: Record<number, string> = {
    [MODE_CODES.ASTROCORRECTION]: 'Астрокоррекция',
    [MODE_CODES.SHOOTING]: 'Съемки',
    [MODE_CODES.OMI]: 'Распр. ОМИ',
    [MODE_CODES.TNP]: 'Режимы ТНП',
    [MODE_CODES.KVD]: 'Калибр. ВД',
    [MODE_CODES.TS]: 'Техн. съемки',
    [MODE_CODES.ONA]: 'Юстировки ОНА'
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
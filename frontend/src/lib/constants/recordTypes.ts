/**
 * Типы записей режимов
 */
export const RECORD_TYPES = {
    KVD: 'kvd',
    TNP: 'tnp',
    TS: 'ts',
    ONA: 'ona',
    OMI: 'omi'
} as const;

export type RecordType = typeof RECORD_TYPES[keyof typeof RECORD_TYPES];
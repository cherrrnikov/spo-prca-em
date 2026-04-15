/**
 * Дефолтные флаги для нового интервала.
 * Используется при создании интервалов из ИД06, вручную и при сборке конфликтов.
 */
export function getDefaultIntervalFlags() {
    return {
        hasConflict: false,
        conflictWith: [] as number[],
        willBeSaved: true,
        nearZasvetka: false,
        zasvetkaConflict: false,
        zasvetkaDistance: 0
    }
}
import type { CreatedProgramData, ProgramModeData } from '$lib/types';
import { TimeUtils } from '$lib/utils/time';

interface MsuInterval {
    created: CreatedProgramData;
    startSeconds: number;
    endSeconds: number;
}

/**
 * Объединяет маленькие интервалы ТС/съемок в большие блоки
 */
export function mergeMsuIntervals(createdPrograms: CreatedProgramData[]): CreatedProgramData[] {
    // 1. Сначала фильтруем все интервалы по willBeSaved
    const savedPrograms = createdPrograms.filter(p => p.timeInterval.willBeSaved === true);
    console.log(`Отфильтровано по willBeSaved: было ${createdPrograms.length}, осталось ${savedPrograms.length}`);
    
    // 2. Отделяем МСУ интервалы (kodMode = 8 или 1)
    const msuPrograms: CreatedProgramData[] = [];
    const otherPrograms: CreatedProgramData[] = [];
    
    for (const program of savedPrograms) {
        const mode = program.modeData.kodMode;
        if (mode === 8 || mode === 1) {
            msuPrograms.push(program);
        } else {
            otherPrograms.push(program);
        }
    }
    
    if (msuPrograms.length === 0) {
        return savedPrograms;
    }
    
    console.log(`Найдено МСУ интервалов для объединения: ${msuPrograms.length}`);
    
    // 3. Группируем по уникальному ключу
    const groups: Map<string, MsuInterval[]> = new Map();
    
    for (const program of msuPrograms) {
        const key = getMsuKey(program.modeData);
        const startSeconds = TimeUtils.timeToSeconds(program.timeInterval.startTime);
        const endSeconds = TimeUtils.timeToSeconds(program.timeInterval.endTime);
        
        if (!groups.has(key)) {
            groups.set(key, []);
        }
        groups.get(key)!.push({ created: program, startSeconds, endSeconds });
    }
    
    console.log(`Сформировано групп для объединения: ${groups.size}`);
    
    // 4. В каждой группе сортируем и объединяем
    const mergedMsuPrograms: CreatedProgramData[] = [];
    
    for (const [key, intervals] of groups.entries()) {
        // Сортируем по времени начала
        intervals.sort((a, b) => a.startSeconds - b.startSeconds);
        
        // Получаем параметры из первого интервала
        const firstModeData = intervals[0].created.modeData;
        const tip = firstModeData.tsData?.tip ?? 1;
        const stepSeconds = tip === 1 ? 30 * 60 : 15 * 60;
        
        console.log(`Группа: tip=${tip}, шаг=${stepSeconds}сек, интервалов=${intervals.length}`);
        
        // Объединяем
        let currentBlock: MsuInterval[] = [intervals[0]];
        
        for (let i = 1; i < intervals.length; i++) {
            const prev = intervals[i - 1];
            const curr = intervals[i];
            
            // Проверяем, идут ли интервалы с правильным шагом между НАЧАЛАМИ
            const expectedStart = prev.startSeconds + stepSeconds;
            const canMerge = Math.abs(curr.startSeconds - expectedStart) < 0.1;
            
            if (canMerge) {
                currentBlock.push(curr);
                console.log(`  Объединен интервал ${i}: начало=${curr.startSeconds}сек, конец=${curr.endSeconds}сек`);
            } else {
                // Закрываем текущий блок
                console.log(`  Блок закрыт, размер=${currentBlock.length}`);
                mergedMsuPrograms.push(createMergedProgram(currentBlock));
                currentBlock = [curr];
            }
        }
        
        // Добавляем последний блок
        if (currentBlock.length > 0) {
            console.log(`  Последний блок, размер=${currentBlock.length}`);
            mergedMsuPrograms.push(createMergedProgram(currentBlock));
        }
    }
    
    console.log(`Результат: было ${msuPrograms.length} интервалов, стало ${mergedMsuPrograms.length} объединенных`);
    
    // 5. Объединяем с остальными программами
    return [...mergedMsuPrograms, ...otherPrograms];
}

/**
 * Создает уникальный ключ для группировки интервалов
 * Все параметры, кроме времени
 */
function getMsuKey(modeData: ProgramModeData): string {
    const ts = modeData.tsData;
    if (!ts) return `no-ts-${modeData.kodMode}-${modeData.numPpi}`;
    
    return JSON.stringify({
        kodMode: modeData.kodMode,
        numPpi: modeData.numPpi,
        zakazchik: modeData.zakazchik,
        tip: ts.tip,
        reg: ts.reg,
        prMsu1: ts.prMsu1,
        vd1Msu1: ts.vd1Msu1,
        vd2Msu1: ts.vd2Msu1,
        vd3Msu1: ts.vd3Msu1,
        ik4Msu1: ts.ik4Msu1,
        ik5Msu1: ts.ik5Msu1,
        ik6Msu1: ts.ik6Msu1,
        ik7Msu1: ts.ik7Msu1,
        ik8Msu1: ts.ik8Msu1,
        ik9Msu1: ts.ik9Msu1,
        ik10Msu1: ts.ik10Msu1,
        prMsu2: ts.prMsu2,
        vd1Msu2: ts.vd1Msu2,
        vd2Msu2: ts.vd2Msu2,
        vd3Msu2: ts.vd3Msu2,
        ik4Msu2: ts.ik4Msu2,
        ik5Msu2: ts.ik5Msu2,
        ik6Msu2: ts.ik6Msu2,
        ik7Msu2: ts.ik7Msu2,
        ik8Msu2: ts.ik8Msu2,
        ik9Msu2: ts.ik9Msu2,
        ik10Msu2: ts.ik10Msu2,
        prBssd: ts.prBssd,
        prZg: ts.prZg,
        prOtklZgBssd: ts.prOtklZgBssd,
        parentId: ts.id  // id из id06_ts для группировки интервалов одного ТС
    });
}

/**
 * Создает объединенный CreatedProgramData из блока интервалов
 */
function createMergedProgram(block: MsuInterval[]): CreatedProgramData {
    const first = block[0].created;
    const last = block[block.length - 1].created;
    
    const startTime = first.timeInterval.startTime;
    const endTime = last.timeInterval.endTime;
    const totalDuration = block.reduce((sum, item) => sum + (item.created.modeData.dlit || 0), 0);
    
    console.log(`  Создан объединенный интервал: ${startTime} - ${endTime}, длительность=${totalDuration}сек`);
    
    // Создаем новый modeData
    const newModeData: ProgramModeData = {
        ...first.modeData,
        dateOn: `${first.modeData.dateOn.split('T')[0]}T${startTime}`,
        dateOff: `${first.modeData.dateOff.split('T')[0]}T${endTime}`,
        dlit: totalDuration,
        tsData: first.modeData.tsData ? { ...first.modeData.tsData, dlit: totalDuration } : undefined
    };
    
    // Создаем новый timeInterval
    const newTimeInterval = {
        ...first.timeInterval,
        startTime,
        endTime,
        dlit: totalDuration
    };
    
    return {
        tempId: `merged_${first.tempId}_${block.length}`,
        modeData: newModeData,
        timeInterval: newTimeInterval
    };
}
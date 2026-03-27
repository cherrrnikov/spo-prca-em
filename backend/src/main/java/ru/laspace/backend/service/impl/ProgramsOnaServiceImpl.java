package ru.laspace.backend.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.programs.ProgramCreateRequest;
import ru.laspace.backend.dto.programs.ProgramCreateRequest.ModeData;
import ru.laspace.backend.entity.programs.ProgramsMain;
import ru.laspace.backend.entity.programs.ProgramsOna;
import ru.laspace.backend.repository.programs.ProgramsOnaRepository;
import ru.laspace.backend.service.ProgramsOnaService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProgramsOnaServiceImpl implements ProgramsOnaService {

    private final ProgramsOnaRepository programsOnaRepository;

    // Режимы, которые используют ОНА1
    private static final List<Integer> ONA1_MODES = List.of(2, 6);
    // Режимы, которые используют ОНА2
    private static final List<Integer> ONA2_MODES = List.of(1, 8, 4, 7, 6);

    @Override
    @Transactional
    public void saveOnaPrograms(ProgramsMain programsMain, ProgramCreateRequest request) {
        log.info("=== ФОРМИРОВАНИЕ PROGRAMs_ONA ===");

        // 1. Собираем все интервалы, которые используют ОНА
        List<OnaInterval> onaIntervals = new ArrayList<>();

        for (ModeData modeData : request.getModes()) {
            Integer kodMode = modeData.getKodMode();
            Integer nOna = determineNOna(modeData);

            if (nOna == null)
                continue; // интервал не использует ОНА

            onaIntervals.add(new OnaInterval(
                    nOna,
                    modeData.getNumPpi(),
                    modeData.getDateOn(),
                    modeData.getDateOff(),
                    kodMode));
        }

        if (onaIntervals.isEmpty()) {
            log.info("Нет интервалов, использующих ОНА");
            return;
        }

        // 2. Сортируем по времени начала
        onaIntervals.sort(Comparator.comparing(OnaInterval::getStart));

        log.info("Найдено интервалов для ОНА: {}", onaIntervals.size());
        for (OnaInterval interval : onaIntervals) {
            log.debug("  ОНА{}: {} - {}, ППИ={}, тип={}",
                    interval.nOna, interval.start, interval.end, interval.ppi, interval.mode);
        }

        // 3. Формируем записи для ОНА1 и ОНА2 отдельно
        List<OnaRecord> ona1Records = buildRecords(onaIntervals, 1);
        List<OnaRecord> ona2Records = buildRecords(onaIntervals, 2);

        // 4. Сохраняем записи
        int savedCount = 0;

        for (OnaRecord record : ona1Records) {
            ProgramsOna ona = new ProgramsOna();
            ona.setProgramsMain(programsMain);
            ona.setNOna(1);
            ona.setDN(record.start);
            ona.setDK(record.end);
            ona.setNPpi(record.ppi);
            ona.setTypeMode(record.firstMode);
            programsOnaRepository.save(ona);
            savedCount++;
            log.debug("Сохранена запись ОНА1: {} - {}, ППИ={}, тип={}",
                    record.start, record.end, record.ppi, record.firstMode);
        }

        for (OnaRecord record : ona2Records) {
            ProgramsOna ona = new ProgramsOna();
            ona.setProgramsMain(programsMain);
            ona.setNOna(2);
            ona.setDN(record.start);
            ona.setDK(record.end);
            ona.setNPpi(record.ppi);
            ona.setTypeMode(record.firstMode);
            programsOnaRepository.save(ona);
            savedCount++;
            log.debug("Сохранена запись ОНА2: {} - {}, ППИ={}, тип={}",
                    record.start, record.end, record.ppi, record.firstMode);
        }

        log.info("Сохранено записей programs_ona: {}", savedCount);
    }

    /**
     * Определяет, какую антенну использует интервал
     * 
     * @return 1 для ОНА1, 2 для ОНА2, null если интервал не использует ОНА
     */
    private Integer determineNOna(ModeData modeData) {
        Integer kodMode = modeData.getKodMode();

        // ОНА1: ОМИ (2) или юстировка ОНА1 (6 с nOna=1)
        if (ONA1_MODES.contains(kodMode)) {
            // Для юстировки ОНА нужно проверить nOna
            if (kodMode == 6 && modeData.getOnaData() != null) {
                return modeData.getOnaData().getNOna() == 1 ? 1 : 2;
            }
            // ОМИ всегда на ОНА1
            if (kodMode == 2)
                return 1;
            // Для других режимов из ONA1_MODES (если будут)
            return 1;
        }

        // ОНА2: съемки (1), ТС (8), ТНП (4), КВД (7), юстировка ОНА2 (6 с nOna=2)
        if (ONA2_MODES.contains(kodMode)) {
            // Для юстировки ОНА нужно проверить nOna
            if (kodMode == 6 && modeData.getOnaData() != null) {
                return modeData.getOnaData().getNOna() == 2 ? 2 : 1;
            }
            return 2;
        }

        return null;
    }

    /**
     * Формирует записи для конкретной антенны
     */
    private List<OnaRecord> buildRecords(List<OnaInterval> allIntervals, int targetOna) {
        // Фильтруем интервалы для нужной антенны
        List<OnaInterval> filtered = allIntervals.stream()
                .filter(i -> i.nOna == targetOna)
                .sorted(Comparator.comparing(i -> i.start))
                .toList();

        if (filtered.isEmpty())
            return List.of();

        List<OnaRecord> records = new ArrayList<>();

        // Начинаем первую группу
        OnaInterval current = filtered.get(0);
        LocalDateTime groupStart = current.start;
        LocalDateTime groupEnd = current.end;
        Integer groupPpi = current.ppi;
        Integer groupFirstMode = current.mode;

        for (int i = 1; i < filtered.size(); i++) {
            OnaInterval next = filtered.get(i);

            // Проверяем только смену ППИ (разрыв во времени не важен)
            boolean samePpi = groupPpi.equals(next.ppi);

            if (samePpi) {
                // Тот же ППИ — объединяем (даже если есть разрыв во времени)
                // Расширяем groupEnd до конца текущего интервала
                if (next.end.isAfter(groupEnd)) {
                    groupEnd = next.end;
                }
            } else {
                // Другой ППИ — закрываем текущую группу и начинаем новую
                records.add(new OnaRecord(groupStart, groupEnd, groupPpi, groupFirstMode));
                groupStart = next.start;
                groupEnd = next.end;
                groupPpi = next.ppi;
                groupFirstMode = next.mode;
            }
        }

        // Добавляем последнюю группу
        records.add(new OnaRecord(groupStart, groupEnd, groupPpi, groupFirstMode));

        return records;
    }

    /**
     * Вспомогательный класс для хранения интервала, использующего ОНА
     */
    private static class OnaInterval {
        final int nOna;
        final int ppi;
        final LocalDateTime start;
        final LocalDateTime end;
        final int mode;

        OnaInterval(int nOna, int ppi, LocalDateTime start, LocalDateTime end, int mode) {
            this.nOna = nOna;
            this.ppi = ppi;
            this.start = start;
            this.end = end;
            this.mode = mode;
        }

        int getNOna() {
            return nOna;
        }

        int getPpi() {
            return ppi;
        }

        LocalDateTime getStart() {
            return start;
        }

        LocalDateTime getEnd() {
            return end;
        }

        int getMode() {
            return mode;
        }
    }

    /**
     * Вспомогательный класс для хранения сформированной записи
     */
    private static class OnaRecord {
        final LocalDateTime start;
        final LocalDateTime end;
        final int ppi;
        final int firstMode;

        OnaRecord(LocalDateTime start, LocalDateTime end, int ppi, int firstMode) {
            this.start = start;
            this.end = end;
            this.ppi = ppi;
            this.firstMode = firstMode;
        }
    }
}
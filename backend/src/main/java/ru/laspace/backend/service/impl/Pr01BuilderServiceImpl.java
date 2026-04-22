package ru.laspace.backend.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.config.PrConfig;
import ru.laspace.backend.entity.programs.ProgramsMain;
import ru.laspace.backend.entity.programs.ProgramsMode;
import ru.laspace.backend.entity.programs.ProgramsModeKvd;
import ru.laspace.backend.entity.programs.ProgramsModeMsu;
import ru.laspace.backend.entity.programs.ProgramsModeOmi;
import ru.laspace.backend.entity.programs.ProgramsModeOna;
import ru.laspace.backend.repository.programs.ProgramsMainRepository;
import ru.laspace.backend.repository.programs.ProgramsModeKvdRepository;
import ru.laspace.backend.repository.programs.ProgramsModeMsuRepository;
import ru.laspace.backend.repository.programs.ProgramsModeOmiRepository;
import ru.laspace.backend.repository.programs.ProgramsModeOnaRepository;
import ru.laspace.backend.repository.programs.ProgramsModeRepository;
import ru.laspace.backend.service.Pr01BuilderService;

@Service
@Slf4j
@RequiredArgsConstructor
public class Pr01BuilderServiceImpl implements Pr01BuilderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH.mm.ss");

    private final ProgramsMainRepository programsMainRepository;
    private final ProgramsModeRepository programsModeRepository;
    private final ProgramsModeMsuRepository programsModeMsuRepository;
    private final ProgramsModeOmiRepository programsModeOmiRepository;
    private final ProgramsModeOnaRepository programsModeOnaRepository;
    private final ProgramsModeKvdRepository programsModeKvdRepository;

    @Override
    public String build(Long numRp, Long numKa, Integer rnf) {
        ProgramsMain main = programsMainRepository.findByNumRpAndNumKa(numRp, numKa)
                .orElseThrow(() -> new RuntimeException(
                        "ПРЦА не найдена: numRp=" + numRp + ", numKa=" + numKa));

        List<ProgramsMode> modes = programsModeRepository
                .findByNumRpAndNumKaOrderByDateOn(numRp, numKa)
                .stream()
                .filter(m -> m.getKodMode() != 9)
                .toList();

        int n = modes.size();
        log.info("Построение ПР01: numRp={}, numKa={}, режимов={}", numRp, numKa, n);

        StringBuilder sb = new StringBuilder();

        // Адресная фраза
        LocalDate planDate = main.getDateOn().toLocalDate();
        long dayNumber = PrConfig.calcDayNumber(planDate);
        String today = LocalDate.now().format(DATE_FMT);

        sb.append("ПР01:")
                .append(numKa).append(",")
                .append(planDate.format(DATE_FMT)).append(",")
                .append(dayNumber).append(",")
                .append(today).append(",")
                .append(numRp).append(",")
                .append(rnf).append(":")
                .append(PrConfig.FORM_PR01_NUMBER).append(";\n");

        // Первая фраза тела — количество режимов
        sb.append("1.").append(n).append(";\n");

        // Фразы режимов — нумерация начинается с 2
        int phraseNum = 2;
        for (ProgramsMode mode : modes) {
            int kodMode = mode.getKodMode();

            // Нечётная фраза — код режима
            sb.append(phraseNum).append(".").append(kodMode).append(";\n");
            phraseNum++;

            // Чётная фраза — параметры
            sb.append(phraseNum).append(".");
            switch (kodMode) {
                case 1, 8 -> sb.append(buildMsuParams(mode));
                case 2 -> sb.append(buildOmiParams(mode));
                case 4 -> sb.append(buildTnpParams(mode));
                case 6 -> sb.append(buildOnaParams(mode));
                case 7 -> sb.append(buildKvdParams(mode));
                default -> {
                    log.warn("Неизвестный kodMode={}, пропускаем параметры", kodMode);
                    sb.append("0");
                }
            }
            sb.append(";\n");
            phraseNum++;
        }

        return sb.toString();
    }

    private String buildMsuParams(ProgramsMode mode) {
        ProgramsModeMsu msu = programsModeMsuRepository
                .findByProgramsModeId(mode.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Данные МСУ не найдены для режима id=" + mode.getId()));

        // Вычисляем количество циклов из хранимых данных
        // tip = 1 (штатная) -> шаг 1800с, иначе 900
        int stepSeconds = (msu.getTip() != null && msu.getTip() == 2) ? 900 : 1800;
        long durationSeconds = Duration.between(mode.getDateOn(), mode.getDateOff()).getSeconds();
        int numCycles = (int) (durationSeconds / stepSeconds) + 1;

        // Время начала последней съемки
        LocalDateTime lastShotStart = mode.getDateOff().minusSeconds(msu.getDlit());

        return fmtDate(mode.getDateOn()) + "," +
                fmtTime(mode.getDateOn()) + "," +
                fmtDate(lastShotStart) + "," +
                fmtTime(lastShotStart) + "," +
                numCycles + "," +
                msu.getTip() + "," +
                msu.getReg() + "," +
                msu.getDlit() + "," +
                "0," +
                msu.getPrMsu1() + "," +
                msu.getPrVdMsu1() + "," +
                msu.getPrIkMsu1() + "," +
                msu.getIk4Msu1() + "," +
                msu.getIk5Msu1() + "," +
                msu.getIk6Msu1() + "," +
                msu.getIk7Msu1() + "," +
                msu.getIk8Msu1() + "," +
                msu.getIk9Msu1() + "," +
                msu.getIk10Msu1() + "," +
                msu.getPrMsu2() + "," +
                msu.getPrVdMsu2() + "," +
                msu.getPrIkMsu2() + "," +
                msu.getIk4Msu2() + "," +
                msu.getIk5Msu2() + "," +
                msu.getIk6Msu2() + "," +
                msu.getIk7Msu2() + "," +
                msu.getIk8Msu2() + "," +
                msu.getIk9Msu2() + "," +
                msu.getIk10Msu2() + "," +
                msu.getPrBssd() + "," +
                msu.getPrZg() + "," +
                "0";
    }

    private String buildOmiParams(ProgramsMode mode) {
        ProgramsModeOmi omi = programsModeOmiRepository
                .findByProgramsModeId(mode.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Данные ОМИ не найдены для режима id=" + mode.getId()));

        return fmtDate(omi.getDateNach()) + "," +
                fmtTime(omi.getDateNach()) + "," +
                fmtDate(omi.getDateCon()) + "," +
                fmtTime(omi.getDateCon());
    }

    private String buildTnpParams(ProgramsMode mode) {
        return fmtDate(mode.getDateOn()) + "," +
                fmtTime(mode.getDateOn()) + "," +
                mode.getDlit();
    }

    private String buildOnaParams(ProgramsMode mode) {
        ProgramsModeOna ona = programsModeOnaRepository
                .findByProgramsModeId(mode.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Данные ОНА не найдены для режима id=" + mode.getId()));

        return fmtDate(ona.getDN()) + "," +
                fmtTime(ona.getDN()) + "," +
                fmtDate(ona.getDK()) + "," +
                fmtTime(ona.getDK()) + "," +
                ona.getNOna() + "," +
                ona.getNPpi();
    }

    private String buildKvdParams(ProgramsMode mode) {
        ProgramsModeKvd kvd = programsModeKvdRepository
                .findByProgramsModeId(mode.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Данные КВД не найдены для режима id=" + mode.getId()));

        return fmtDate(mode.getDateOn()) + "," +
                fmtTime(mode.getDateOn()) + "," +
                kvd.getPrMsu() + "," +
                kvd.getPrBssd() + "," +
                kvd.getPrZg();
    }

    private String fmtDate(LocalDateTime dt) {
        return dt.format(DATE_FMT);
    }

    private String fmtTime(LocalDateTime dt) {
        return dt.format(TIME_FMT);
    }
}
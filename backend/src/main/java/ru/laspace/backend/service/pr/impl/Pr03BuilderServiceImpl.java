package ru.laspace.backend.service.pr.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.config.PrConfig;
import ru.laspace.backend.entity.programs.ProgramsMain;
import ru.laspace.backend.entity.programs.ProgramsOna;
import ru.laspace.backend.repository.programs.ProgramsMainRepository;
import ru.laspace.backend.repository.programs.ProgramsOnaRepository;
import ru.laspace.backend.service.pr.Pr03BuilderService;

@Service
@Slf4j
@RequiredArgsConstructor
public class Pr03BuilderServiceImpl implements Pr03BuilderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH.mm.ss");

    private final ProgramsMainRepository programsMainRepository;
    private final ProgramsOnaRepository programsOnaRepository;

    @Override
    public String build(Integer numRp, Integer numKa, Integer rnf) {
        ProgramsMain main = programsMainRepository.findByNumRpAndNumKa(numRp, numKa)
                .orElseThrow(() -> new RuntimeException(
                        "ПРЦА не найдена: numRp=" + numRp + ", numKa=" + numKa));

        // Берём только записи ОНА1 (nOna=1), уже отсортированные по dN
        List<ProgramsOna> records = programsOnaRepository
                .findByNumRpAndNumKaAndNOnaOrderByDN(numRp, numKa, 1);

        int n = records.size();
        log.info("Построение ПР03: numRp={}, numKa={}, записей ОНА1={}", numRp, numKa, n);

        StringBuilder sb = new StringBuilder();

        // Адресная фраза
        LocalDate planDate = main.getDateOn().toLocalDate();
        long dayNumber = PrConfig.calcDayNumber(planDate);
        String today = LocalDate.now().format(DATE_FMT);

        sb.append("ПР03:")
                .append(numKa).append(",")
                .append(planDate.format(DATE_FMT)).append(",")
                .append(dayNumber).append(",")
                .append(today).append(",")
                .append(numRp).append(",")
                .append(rnf).append(":")
                .append(PrConfig.FORM_PR03_NUMBER).append(";\n");

        // Первая фраза тела
        sb.append("1.").append(n).append(",0;\n");

        // Фразы интервалов
        int phraseNum = 2;
        for (ProgramsOna rec : records) {
            sb.append(phraseNum).append(".")
                    .append(fmtDate(rec.getDN())).append(",")
                    .append(fmtTime(rec.getDN())).append(",")
                    .append(fmtDate(rec.getDK())).append(",")
                    .append(fmtTime(rec.getDK())).append(",")
                    .append(rec.getNPpi()).append(",")
                    .append(rec.getTypeMode()).append(";\n");
            phraseNum++;
        }

        return sb.toString();
    }

    private String fmtDate(LocalDateTime dt) {
        return dt.format(DATE_FMT);
    }

    private String fmtTime(LocalDateTime dt) {
        return dt.format(TIME_FMT);
    }
}
package ru.laspace.backend.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.config.PrConfig;
import ru.laspace.backend.entity.vp.Vp01;
import ru.laspace.backend.entity.vp.Vp01Kvd;
import ru.laspace.backend.entity.vp.Vp01Msu;
import ru.laspace.backend.entity.vp.Vp01Omi;
import ru.laspace.backend.entity.vp.Vp01Ona;
import ru.laspace.backend.entity.vp.Vp01Tnp;
import ru.laspace.backend.repository.FormInRepository;
import ru.laspace.backend.repository.vp.Vp01KvdRepository;
import ru.laspace.backend.repository.vp.Vp01MsuRepository;
import ru.laspace.backend.repository.vp.Vp01OmiRepository;
import ru.laspace.backend.repository.vp.Vp01OnaRepository;
import ru.laspace.backend.repository.vp.Vp01Repository;
import ru.laspace.backend.repository.vp.Vp01TnpRepository;
import ru.laspace.backend.service.Vp01BuilderService;

@Service
@Slf4j
@RequiredArgsConstructor
public class Vp01BuilderServiceImpl implements Vp01BuilderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH.mm.ss");

    // ident_n для НУ04 в таблице form_in
    private static final int IDENT_N_NU04 = 16;

    // Коды массивов ВП01
    private static final int ARRAY_MSU = 1;
    private static final int ARRAY_OMI = 2;
    private static final int ARRAY_RELAY = 3;
    private static final int ARRAY_ONA = 4;
    private static final int ARRAY_TNP = 5;
    private static final int ARRAY_TECH_MSU = 6;
    private static final int ARRAY_CA_JUST = 7;
    private static final int ARRAY_KVD = 8;

    private final Vp01Repository vp01Repository;
    private final Vp01MsuRepository vp01MsuRepository;
    private final Vp01OmiRepository vp01OmiRepository;
    private final Vp01OnaRepository vp01OnaRepository;
    private final Vp01TnpRepository vp01TnpRepository;
    private final Vp01KvdRepository vp01KvdRepository;
    private final FormInRepository formInRepository;

    @Override
    public String build(Long numRp, Long numKa, Integer rnf) {
        // Находим главную запись ВПРЦА
        Vp01 vp01 = vp01Repository.findByNumRpAndNumKa(numRp, numKa)
                .orElseThrow(() -> new RuntimeException(
                        "ВПРЦА не найдена: numRp=" + numRp + ", numKa=" + numKa));

        // Загружаем все подзаписи
        List<Vp01Msu> allMsu = vp01MsuRepository.findByVp01IdOrderByNumMsu(vp01.getId());
        List<Vp01Msu> msuStandard = allMsu.stream().filter(m -> m.getTip() != null && m.getTip() == 1).toList();
        List<Vp01Msu> msuTech = allMsu.stream().filter(m -> m.getTip() != null && m.getTip() == 2).toList();
        List<Vp01Omi> omiList = vp01OmiRepository.findByVp01IdOrderByNumOmi(vp01.getId());
        List<Vp01Ona> onaList = vp01OnaRepository.findByVp01IdOrderByNumUstOna(vp01.getId());
        List<Vp01Tnp> tnpList = vp01TnpRepository.findByVp01IdOrderByNumTnp(vp01.getId());
        List<Vp01Kvd> kvdList = vp01KvdRepository.findByVp01IdOrderByNumKvd(vp01.getId());

        log.info("Построение ВП01: numRp={}, numKa={}, rnf={}", numRp, numKa, rnf);
        log.info("МСУ штатных={}, технол={}, ОМИ={}, ОНА={}, ТНП={}, КВД={}",
                msuStandard.size(), msuTech.size(), omiList.size(),
                onaList.size(), tnpList.size(), kvdList.size());

        StringBuilder sb = new StringBuilder();

        // Адресная фраза
        LocalDate planDate = vp01.getDtNRp().toLocalDate();
        long dayNumber = PrConfig.calcDayNumber(planDate);
        String today = LocalDate.now().format(DATE_FMT);

        sb.append("ВП01:")
                .append(numKa).append(",")
                .append(planDate.format(DATE_FMT)).append(",")
                .append(dayNumber).append(",").append(today).append(",")
                .append(numRp).append(",")
                .append(rnf).append(":")
                .append(PrConfig.FORM_VP01_NUMBER).append(";\n");

        // Фразы 1-15: блок НУ04 из form_in
        String nu04 = formInRepository.findLatestContentByIdentN(IDENT_N_NU04);
        if (nu04 != null) {
            sb.append(nu04);
            if (!nu04.endsWith("\n")) {
                sb.append("\n");
            }
        } else {
            log.warn("НУ04 не найдена в form_in, блок начальных условий будет пустым");
        }

        // Нумерация фраз начинается с 16
        int phraseNum = 16;

        // Массив 1 — штатные съёмки МСУ-ГС (tip=1)
        sb.append(phraseNum).append(".").append(ARRAY_MSU).append(",").append(msuStandard.size()).append(";\n");
        phraseNum++;
        for (int i = 0; i < msuStandard.size(); i++) {
            sb.append(phraseNum).append(".").append(buildMsuPhrase(i + 1, msuStandard.get(i))).append(";\n");
            phraseNum++;
        }

        // Массив 2 — ОМИ
        sb.append(phraseNum).append(".").append(ARRAY_OMI).append(",").append(omiList.size()).append(";\n");
        phraseNum++;
        for (int i = 0; i < omiList.size(); i++) {
            sb.append(phraseNum).append(".").append(buildOmiPhrase(i + 1, omiList.get(i))).append(";\n");
            phraseNum++;
        }

        // Массив 3 — ретрансляция (данных нет, r=0)
        sb.append(phraseNum).append(".").append(ARRAY_RELAY).append(",").append(0).append(";\n");
        phraseNum++;

        // Массив 4 — юстировка ОНА
        sb.append(phraseNum).append(".").append(ARRAY_ONA).append(",").append(onaList.size()).append(";\n");
        phraseNum++;
        for (int i = 0; i < onaList.size(); i++) {
            sb.append(phraseNum).append(".").append(buildOnaPhrase(i + 1, onaList.get(i))).append(";\n");
            phraseNum++;
        }

        // Массив 5 — ТНП
        sb.append(phraseNum).append(".").append(ARRAY_TNP).append(",").append(tnpList.size()).append(";\n");
        phraseNum++;
        for (int i = 0; i < tnpList.size(); i++) {
            sb.append(phraseNum).append(".").append(buildTnpPhrase(i + 1, tnpList.get(i))).append(";\n");
            phraseNum++;
        }

        // Массив 6 — технологические съёмки МСУ-ГС (tip=2)
        sb.append(phraseNum).append(".").append(ARRAY_TECH_MSU).append(",").append(msuTech.size()).append(";\n");
        phraseNum++;
        for (int i = 0; i < msuTech.size(); i++) {
            sb.append(phraseNum).append(".").append(buildMsuPhrase(i + 1, msuTech.get(i))).append(";\n");
            phraseNum++;
        }

        // Массив 7 — юстировка ЦА (МСУ-ГС), данных нет, g=0
        sb.append(phraseNum).append(".").append(ARRAY_CA_JUST).append(",").append(0).append(";\n");
        phraseNum++;

        // Массив 8 — калибровка ВД
        sb.append(phraseNum).append(".").append(ARRAY_KVD).append(",").append(kvdList.size()).append(";\n");
        phraseNum++;
        for (int i = 0; i < kvdList.size(); i++) {
            sb.append(phraseNum).append(".").append(buildKvdPhrase(i + 1, kvdList.get(i))).append(";\n");
            phraseNum++;
        }

        return sb.toString();
    }

    // 28 предложений: номер, дата/время начала, дата/время конца, 11 флагов МСУ1,
    // 11 флагов МСУ2, numPpi
    private String buildMsuPhrase(int num, Vp01Msu msu) {
        return num + "," +
                fmtDate(msu.getDateNach()) + "," +
                fmtTime(msu.getDateNach()) + "," +
                fmtDate(msu.getDateCon()) + "," +
                fmtTime(msu.getDateCon()) + "," +
                msu.getComplectMsu1() + "," +
                msu.getVd11() + "," +
                msu.getVd12() + "," +
                msu.getVd13() + "," +
                msu.getIk14() + "," +
                msu.getIk15() + "," +
                msu.getIk16() + "," +
                msu.getIk17() + "," +
                msu.getIk18() + "," +
                msu.getIk19() + "," +
                msu.getIk110() + "," +
                msu.getComplectMsu2() + "," +
                msu.getVd21() + "," +
                msu.getVd22() + "," +
                msu.getVd23() + "," +
                msu.getIk24() + "," +
                msu.getIk25() + "," +
                msu.getIk26() + "," +
                msu.getIk27() + "," +
                msu.getIk28() + "," +
                msu.getIk29() + "," +
                msu.getIk210() + "," +
                msu.getNumPpi();
    }

    // 7 предложений: номер, typeOmi, дата/время начала, дата/время конца, numPpi
    private String buildOmiPhrase(int num, Vp01Omi omi) {
        return num + "," +
                omi.getTypeOmi() + "," +
                fmtDate(omi.getDateNach()) + "," +
                fmtTime(omi.getDateNach()) + "," +
                fmtDate(omi.getDateCon()) + "," +
                fmtTime(omi.getDateCon()) + "," +
                omi.getNumPpi();
    }

    // 7 предложений: номер, дата/время начала, дата/время конца, numUstOna, numPpi
    private String buildOnaPhrase(int num, Vp01Ona ona) {
        return num + "," +
                fmtDate(ona.getDateNach()) + "," +
                fmtTime(ona.getDateNach()) + "," +
                fmtDate(ona.getDateCon()) + "," +
                fmtTime(ona.getDateCon()) + "," +
                ona.getNumUstOna() + "," +
                ona.getNumPpi();
    }

    // 6 предложений: номер, дата/время начала, дата/время конца, numPpi
    private String buildTnpPhrase(int num, Vp01Tnp tnp) {
        return num + "," +
                fmtDate(tnp.getDateNach()) + "," +
                fmtTime(tnp.getDateNach()) + "," +
                fmtDate(tnp.getDateCon()) + "," +
                fmtTime(tnp.getDateCon()) + "," +
                tnp.getNumPpi();
    }

    // 7 предложений: номер, дата/время начала, дата/время конца, complectMsu,
    // numPpi
    private String buildKvdPhrase(int num, Vp01Kvd kvd) {
        return num + "," +
                fmtDate(kvd.getDateNach()) + "," +
                fmtTime(kvd.getDateNach()) + "," +
                fmtDate(kvd.getDateCon()) + "," +
                fmtTime(kvd.getDateCon()) + "," +
                kvd.getComplectMsu() + "," +
                kvd.getNumPpi();
    }

    private String fmtDate(LocalDateTime dt) {
        return dt.format(DATE_FMT);
    }

    private String fmtTime(LocalDateTime dt) {
        return dt.format(TIME_FMT);
    }
}
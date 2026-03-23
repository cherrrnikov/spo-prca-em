package ru.laspace.backend.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.programs.ProgramCreateRequest;
import ru.laspace.backend.dto.programs.ProgramCreateRequest.ModeData;
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
import ru.laspace.backend.service.ProgramsService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProgramsServiceImpl implements ProgramsService {

    private final ProgramsMainRepository programsMainRepository;
    private final ProgramsModeRepository programsModeRepository;
    private final ProgramsModeKvdRepository programsModeKvdRepository;
    private final ProgramsModeMsuRepository programsModeMsuRepository;
    private final ProgramsModeOmiRepository programsModeOmiRepository;
    private final ProgramsModeOnaRepository programsModeOnaRepository;

    @Override
    @Transactional
    public void saveProgram(ProgramCreateRequest request) {
        log.info("=== Начало сохранения ПРЦА ===");
        log.info("Номер ПРЦА: {}, Номер КА: {}",
                request.getMainData().getNumRp(),
                request.getMainData().getNumKa());
        log.info("Всего режимов в запросе: {}", request.getModes().size());

        // 1. Сохраняем ProgramsMain
        ProgramsMain programsMain = createProgramsMain(request.getMainData());
        ProgramsMain savedMain = programsMainRepository.save(programsMain);
        log.info("Сохранена ProgramsMain с id = {}", savedMain.getId());

        // 2. Сохраняем каждый режим
        int savedCount = 0;
        int skippedCount = 0; // временно пока не сохраняем ТС
        for (ModeData modeData : request.getModes()) {
            Integer kodMode = modeData.getKodMode();

            // // Временно пропускаем ТС до реализации объединения подинтервалов
            // if (kodMode == 8) {
            // log.warn("ТС (kodMode=8) временно пропускаем, ждем реализации объединения
            // интервалов");
            // skippedCount++;
            // continue;
            // }

            try {
                saveMode(savedMain, modeData);
                savedCount++;
                log.debug("Сохранен режим: kodMode={}, время={}-{}",
                        kodMode, modeData.getDateOn(), modeData.getDateOff());
            } catch (Exception e) {
                log.error("Ошибка при сохранении режима kodMode={}: {}", kodMode, e.getMessage(), e);
                throw new RuntimeException("Ошибка сохранения режима", e);
            }
        }

        log.info("=== Сохранение ПРЦА завершено ===");
        log.info("Сохранено режимов: {}, Пропущено ТС: {}", savedCount, skippedCount);
    }

    private ProgramsMain createProgramsMain(ProgramCreateRequest.MainData mainData) {
        ProgramsMain programsMain = new ProgramsMain();
        programsMain.setNumRp(mainData.getNumRp());
        programsMain.setNumKa(mainData.getNumKa());
        programsMain.setDateOn(mainData.getDateOn());
        programsMain.setDateOff(mainData.getDateOff());
        programsMain.setTypeRp(mainData.getTypeRp());
        programsMain.setPrOtpr(mainData.getPrOtpr());
        return programsMain;
    }

    private void saveMode(ProgramsMain programsMain, ModeData modeData) {
        // 2.1. Сохраняем ProgramsMode
        ProgramsMode programsMode = new ProgramsMode();
        programsMode.setProgramsMain(programsMain);
        programsMode.setNumRp(modeData.getNumRp());
        programsMode.setNumKa(modeData.getNumKa());
        programsMode.setDateOn(modeData.getDateOn());
        programsMode.setDateOff(modeData.getDateOff());
        programsMode.setKodMode(modeData.getKodMode());
        programsMode.setNumPpi(modeData.getNumPpi());
        programsMode.setDlit(modeData.getDlit());
        programsMode.setZakazchik(modeData.getZakazchik());

        ProgramsMode savedMode = programsModeRepository.save(programsMode);
        log.debug("Сохранен ProgramsMode id={}, kodMode={}", savedMode.getId(), savedMode.getKodMode());

        // 2.2. Сохраняем детальные данные в зависимости от типа режима
        Integer kodMode = modeData.getKodMode();

        if (kodMode == 7 && modeData.getKvdData() != null) {
            saveKvdData(savedMode, modeData.getKvdData());
        } else if ((kodMode == 8 || kodMode == 1) && modeData.getTsData() != null) {
            // ТС (8) и обычные съемки (1) — сохраняем в programs_mode_msu
            saveMsuData(savedMode, modeData.getTsData());
        } else if (kodMode == 2 && modeData.getOmiData() != null) {
            saveOmiData(savedMode, modeData.getOmiData());
        } else if (kodMode == 6 && modeData.getOnaData() != null) {
            saveOnaData(savedMode, modeData.getOnaData());
        }
        // kodMode == 4 (ТНП) - нет детальной таблицы
        else {
            log.debug("Режим {} не имеет детальной таблицы или данные отсутствуют", kodMode);
        }
    }

    private void saveKvdData(ProgramsMode programsMode, ProgramCreateRequest.KvdData kvdData) {
        ProgramsModeKvd kvd = new ProgramsModeKvd();
        kvd.setProgramsMode(programsMode);
        kvd.setPrMsu(kvdData.getPrMsu());
        kvd.setPrBssd(kvdData.getPrBssd());
        kvd.setPrZg(kvdData.getPrZg());
        programsModeKvdRepository.save(kvd);
        log.debug("Сохранены данные КВД для режима id={}", programsMode.getId());
    }

    private void saveMsuData(ProgramsMode programsMode, ProgramCreateRequest.TsData tsData) {
        ProgramsModeMsu msu = new ProgramsModeMsu();
        msu.setProgramsMode(programsMode);
        msu.setTip(tsData.getTip());
        msu.setReg(tsData.getReg());
        msu.setDlit(tsData.getDlit());

        msu.setPrMsu1(tsData.getPrMsu1());
        msu.setPrVdMsu1(tsData.getVd1Msu1() != null
                && (tsData.getVd1Msu1() == 1 || tsData.getVd2Msu1() == 1 || tsData.getVd3Msu1() == 1) ? 1 : 0);
        msu.setPrIkMsu1(tsData.getIk4Msu1() != null && (tsData.getIk4Msu1() == 1 || tsData.getIk5Msu1() == 1 ||
                tsData.getIk6Msu1() == 1 || tsData.getIk7Msu1() == 1 || tsData.getIk8Msu1() == 1 ||
                tsData.getIk9Msu1() == 1 || tsData.getIk10Msu1() == 1) ? 1 : 0);

        msu.setVd1Msu1(tsData.getVd1Msu1());
        msu.setVd2Msu1(tsData.getVd2Msu1());
        msu.setVd3Msu1(tsData.getVd3Msu1());

        msu.setIk4Msu1(tsData.getIk4Msu1());
        msu.setIk5Msu1(tsData.getIk5Msu1());
        msu.setIk6Msu1(tsData.getIk6Msu1());
        msu.setIk7Msu1(tsData.getIk7Msu1());
        msu.setIk8Msu1(tsData.getIk8Msu1());
        msu.setIk9Msu1(tsData.getIk9Msu1());
        msu.setIk10Msu1(tsData.getIk10Msu1());

        msu.setPrMsu2(tsData.getPrMsu2());
        msu.setPrVdMsu2(tsData.getVd1Msu2() != null
                && (tsData.getVd1Msu2() == 1 || tsData.getVd2Msu2() == 1 || tsData.getVd3Msu2() == 1) ? 1 : 0);
        msu.setPrIkMsu2(tsData.getIk4Msu2() != null && (tsData.getIk4Msu2() == 1 || tsData.getIk5Msu2() == 1 ||
                tsData.getIk6Msu2() == 1 || tsData.getIk7Msu2() == 1 || tsData.getIk8Msu2() == 1 ||
                tsData.getIk9Msu2() == 1 || tsData.getIk10Msu2() == 1) ? 1 : 0);

        msu.setVd1Msu2(tsData.getVd1Msu2());
        msu.setVd2Msu2(tsData.getVd2Msu2());
        msu.setVd3Msu2(tsData.getVd3Msu2());

        msu.setIk4Msu2(tsData.getIk4Msu2());
        msu.setIk5Msu2(tsData.getIk5Msu2());
        msu.setIk6Msu2(tsData.getIk6Msu2());
        msu.setIk7Msu2(tsData.getIk7Msu2());
        msu.setIk8Msu2(tsData.getIk8Msu2());
        msu.setIk9Msu2(tsData.getIk9Msu2());
        msu.setIk10Msu2(tsData.getIk10Msu2());

        msu.setPrBssd(tsData.getPrBssd());
        msu.setPrZg(tsData.getPrZg());
        msu.setPrOtklZgBssd(tsData.getPrOtklZgBssd());

        programsModeMsuRepository.save(msu);
        log.debug("Сохранены данные МСУ для режима id={}", programsMode.getId());
    }

    private void saveOmiData(ProgramsMode programsMode, ProgramCreateRequest.OmiData omiData) {
        ProgramsModeOmi omi = new ProgramsModeOmi();
        omi.setProgramsMode(programsMode);
        omi.setNumOmi(omiData.getNumOmi());
        omi.setTypeOmi(omiData.getTypeOmi());
        omi.setDateNach(omiData.getDateNach());
        omi.setDateCon(omiData.getDateCon());
        omi.setDlit(omiData.getDlit());
        programsModeOmiRepository.save(omi);
        log.debug("Сохранены данные ОМИ для режима id={}", programsMode.getId());
    }

    private void saveOnaData(ProgramsMode programsMode, ProgramCreateRequest.OnaData onaData) {
        ProgramsModeOna ona = new ProgramsModeOna();
        ona.setProgramsMode(programsMode);
        ona.setDN(onaData.getDN());
        ona.setDK(onaData.getDK());
        ona.setNOna(onaData.getNOna());
        ona.setNPpi(onaData.getNPpi());
        programsModeOnaRepository.save(ona);
        log.debug("Сохранены данные ОНА для режима id={}", programsMode.getId());
    }

}

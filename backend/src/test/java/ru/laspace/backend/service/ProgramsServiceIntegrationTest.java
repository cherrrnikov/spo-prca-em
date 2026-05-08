package ru.laspace.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import ru.laspace.backend.TestcontainersConfiguration;
import ru.laspace.backend.dto.programs.ProgramCreateRequest;
import ru.laspace.backend.entity.programs.ProgramsMain;
import ru.laspace.backend.repository.programs.ProgramsMainRepository;
import ru.laspace.backend.repository.programs.ProgramsModeMsuRepository;
import ru.laspace.backend.repository.programs.ProgramsModeRepository;
import ru.laspace.backend.service.programs.ProgramsService;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ProgramsServiceIntegrationTest {

    @Autowired
    private ProgramsService programsService;

    @Autowired
    private ProgramsMainRepository programsMainRepository;

    @Autowired
    private ProgramsModeRepository programsModeRepository;

    @Autowired
    private ProgramsModeMsuRepository programsModeMsuRepository;

    @BeforeEach
    void cleanUp() {
        programsModeMsuRepository.deleteAll();
        programsModeRepository.deleteAll();
        programsMainRepository.deleteAll();
    }

    @Test
    void saveProgram_noModes_savesMainRecord() {
        ProgramCreateRequest request = buildRequest(1525, List.of());

        Integer numRp = programsService.saveProgram(request);

        assertThat(numRp).isEqualTo(1);
        Optional<ProgramsMain> saved = programsMainRepository
                .findByNumRpAndNumKa(numRp, 1525);
        assertThat(saved).isPresent();
        assertThat(saved.get().getNumKa()).isEqualTo(1525);
    }

    @Test
    void saveProgram_withMsuMode_savesModeAndMsuData() {
        ProgramCreateRequest.MsuData msuData = buildMsuData();
        ProgramCreateRequest.ModeData modeData = buildModeData(1, msuData);
        ProgramCreateRequest request = buildRequest(1525, List.of(modeData));

        Integer numRp = programsService.saveProgram(request);

        assertThat(numRp).isEqualTo(1);
        var modes = programsModeRepository.findByNumRpAndNumKaOrderByDateOn(numRp, 1525);
        assertThat(modes).hasSize(1);
        assertThat(modes.get(0).getKodMode()).isEqualTo(1);

        var msuList = programsModeMsuRepository
                .findByProgramsModeId(modes.get(0).getId());
        assertThat(msuList).isPresent();
        assertThat(msuList.get().getTip()).isEqualTo(1);
    }

    @Test
    void saveProgram_secondProgram_incrementsNumRp() {
        ProgramCreateRequest request1 = buildRequest(1525, List.of());
        ProgramCreateRequest request2 = buildRequest(1525, List.of());

        Integer numRp1 = programsService.saveProgram(request1);
        Integer numRp2 = programsService.saveProgram(request2);

        assertThat(numRp1).isEqualTo(1);
        assertThat(numRp2).isEqualTo(2);
    }

    @Test
    void saveProgram_differentKa_independentNumbering() {
        ProgramCreateRequest request1525 = buildRequest(1525, List.of());
        ProgramCreateRequest request1526 = buildRequest(1526, List.of());

        Integer numRp1 = programsService.saveProgram(request1525);
        Integer numRp2 = programsService.saveProgram(request1526);

        assertThat(numRp1).isEqualTo(1);
        assertThat(numRp2).isEqualTo(1);
    }

    @Test
    void saveProgram_multipleModes_allSaved() {
        ProgramCreateRequest.MsuData msuData = buildMsuData();
        ProgramCreateRequest.ModeData msuMode = buildModeData(1, msuData);
        ProgramCreateRequest.ModeData tnpMode = buildModeData(4, null);
        tnpMode.setDateOn(LocalDateTime.of(2026, 4, 30, 5, 0));
        tnpMode.setDateOff(LocalDateTime.of(2026, 4, 30, 5, 12));
        tnpMode.setDlit(516);

        ProgramCreateRequest request = buildRequest(1525, List.of(msuMode, tnpMode));

        Integer numRp = programsService.saveProgram(request);

        var modes = programsModeRepository.findByNumRpAndNumKaOrderByDateOn(numRp, 1525);
        assertThat(modes).hasSize(2);
        assertThat(modes).extracting("kodMode")
                .containsExactlyInAnyOrder(1, 4);
    }

    // --- вспомогательные методы ---

    private ProgramCreateRequest buildRequest(Integer numKa,
            List<ProgramCreateRequest.ModeData> modes) {
        ProgramCreateRequest.MainData mainData = new ProgramCreateRequest.MainData();
        mainData.setNumKa(numKa);
        mainData.setDateOn(LocalDateTime.of(2026, 4, 30, 0, 0));
        mainData.setDateOff(LocalDateTime.of(2026, 4, 30, 23, 59));
        mainData.setTypeRp(3);
        mainData.setPrOtpr(0);

        ProgramCreateRequest request = new ProgramCreateRequest();
        request.setMainData(mainData);
        request.setModes(modes);
        return request;
    }

    private ProgramCreateRequest.ModeData buildModeData(int kodMode,
            ProgramCreateRequest.MsuData msuData) {
        ProgramCreateRequest.ModeData mode = new ProgramCreateRequest.ModeData();
        mode.setNumKa(1525);
        mode.setKodMode(kodMode);
        mode.setDateOn(LocalDateTime.of(2026, 4, 30, 0, 0));
        mode.setDateOff(LocalDateTime.of(2026, 4, 30, 4, 45));
        mode.setNumPpi(1);
        mode.setDlit(420);
        mode.setMsuData(msuData);
        return mode;
    }

    private ProgramCreateRequest.MsuData buildMsuData() {
        ProgramCreateRequest.MsuData msu = new ProgramCreateRequest.MsuData();
        msu.setTip(1);
        msu.setReg(1);
        msu.setDlit(420);
        msu.setPrMsu1(1);
        msu.setVd1Msu1(1);
        msu.setVd2Msu1(1);
        msu.setVd3Msu1(1);
        msu.setIk4Msu1(1);
        msu.setIk5Msu1(1);
        msu.setIk6Msu1(1);
        msu.setIk7Msu1(1);
        msu.setIk8Msu1(1);
        msu.setIk9Msu1(1);
        msu.setIk10Msu1(1);
        msu.setPrMsu2(0);
        msu.setVd1Msu2(0);
        msu.setVd2Msu2(0);
        msu.setVd3Msu2(0);
        msu.setIk4Msu2(0);
        msu.setIk5Msu2(0);
        msu.setIk6Msu2(0);
        msu.setIk7Msu2(0);
        msu.setIk8Msu2(0);
        msu.setIk9Msu2(0);
        msu.setIk10Msu2(0);
        msu.setPrBssd(1);
        msu.setPrZg(1);
        msu.setPrOtklZgBssd(0);
        return msu;
    }
}

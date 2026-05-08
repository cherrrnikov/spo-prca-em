package ru.laspace.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.laspace.backend.entity.programs.ProgramsMain;
import ru.laspace.backend.entity.programs.ProgramsMode;
import ru.laspace.backend.entity.programs.ProgramsModeKvd;
import ru.laspace.backend.entity.programs.ProgramsModeMsu;
import ru.laspace.backend.entity.programs.ProgramsModeOmi;
import ru.laspace.backend.repository.programs.ProgramsMainRepository;
import ru.laspace.backend.repository.programs.ProgramsModeKvdRepository;
import ru.laspace.backend.repository.programs.ProgramsModeMsuRepository;
import ru.laspace.backend.repository.programs.ProgramsModeOmiRepository;
import ru.laspace.backend.repository.programs.ProgramsModeOnaRepository;
import ru.laspace.backend.repository.programs.ProgramsModeRepository;
import ru.laspace.backend.service.pr.impl.Pr01BuilderServiceImpl;

@ExtendWith(MockitoExtension.class)
class Pr01BuilderServiceImplTest {

    @Mock
    private ProgramsMainRepository programsMainRepository;
    @Mock
    private ProgramsModeRepository programsModeRepository;
    @Mock
    private ProgramsModeMsuRepository programsModeMsuRepository;
    @Mock
    private ProgramsModeOmiRepository programsModeOmiRepository;
    @Mock
    private ProgramsModeOnaRepository programsModeOnaRepository;
    @Mock
    private ProgramsModeKvdRepository programsModeKvdRepository;

    @InjectMocks
    private Pr01BuilderServiceImpl pr01BuilderService;

    private ProgramsMain main;

    @BeforeEach
    void setUp() {
        main = new ProgramsMain();
        main.setId(1L);
        main.setNumRp(1);
        main.setNumKa(1525);
        main.setDateOn(LocalDateTime.of(2026, 4, 30, 0, 0));
        main.setDateOff(LocalDateTime.of(2026, 4, 30, 23, 59));
    }

    // --- адресная фраза ---

    @Test
    void build_containsCorrectHeader() {
        ProgramsMode msuMode = buildMode(1L, 1,
                LocalDateTime.of(2026, 4, 30, 0, 0),
                LocalDateTime.of(2026, 4, 30, 4, 45));
        ProgramsModeMsu msu = buildMsu(msuMode, 1, 1, 420);

        when(programsMainRepository.findByNumRpAndNumKa(1, 1525))
                .thenReturn(Optional.of(main));
        when(programsModeRepository.findByNumRpAndNumKaOrderByDateOn(1, 1525))
                .thenReturn(List.of(msuMode));
        when(programsModeMsuRepository.findByProgramsModeId(1L))
                .thenReturn(Optional.of(msu));

        String result = pr01BuilderService.build(1, 1525, 1);

        assertThat(result).startsWith("ПР01:1525,");
        assertThat(result).contains(":003;");
    }

    // --- количество режимов ---

    @Test
    void build_firstPhraseContainsCorrectModeCount() {
        ProgramsMode msuMode = buildMode(1L, 1,
                LocalDateTime.of(2026, 4, 30, 0, 0),
                LocalDateTime.of(2026, 4, 30, 4, 45));
        ProgramsModeMsu msu = buildMsu(msuMode, 1, 1, 420);

        when(programsMainRepository.findByNumRpAndNumKa(1, 1525))
                .thenReturn(Optional.of(main));
        when(programsModeRepository.findByNumRpAndNumKaOrderByDateOn(1, 1525))
                .thenReturn(List.of(msuMode));
        when(programsModeMsuRepository.findByProgramsModeId(1L))
                .thenReturn(Optional.of(msu));

        String result = pr01BuilderService.build(1, 1525, 1);

        assertThat(result).contains("\n1.1;\n");
    }

    // --- режим МСУ (kodMode=1) ---

    @Test
    void build_msuMode_containsKodModeAndParams() {
        ProgramsMode msuMode = buildMode(1L, 1,
                LocalDateTime.of(2026, 4, 30, 0, 0),
                LocalDateTime.of(2026, 4, 30, 4, 45));
        ProgramsModeMsu msu = buildMsu(msuMode, 1, 1, 420);

        when(programsMainRepository.findByNumRpAndNumKa(1, 1525))
                .thenReturn(Optional.of(main));
        when(programsModeRepository.findByNumRpAndNumKaOrderByDateOn(1, 1525))
                .thenReturn(List.of(msuMode));
        when(programsModeMsuRepository.findByProgramsModeId(1L))
                .thenReturn(Optional.of(msu));

        String result = pr01BuilderService.build(1, 1525, 1);

        assertThat(result).contains("\n2.1;\n"); // код режима
        assertThat(result).contains("30.04.2026"); // дата начала
        assertThat(result).contains("00.00.00"); // время начала
    }

    // --- режим ТНП (kodMode=4) ---

    @Test
    void build_tnpMode_containsKodModeAndParams() {
        ProgramsMode tnpMode = buildMode(2L, 4,
                LocalDateTime.of(2026, 4, 30, 5, 0),
                LocalDateTime.of(2026, 4, 30, 5, 12));
        tnpMode.setDlit(516);

        when(programsMainRepository.findByNumRpAndNumKa(1, 1525))
                .thenReturn(Optional.of(main));
        when(programsModeRepository.findByNumRpAndNumKaOrderByDateOn(1, 1525))
                .thenReturn(List.of(tnpMode));

        String result = pr01BuilderService.build(1, 1525, 1);

        assertThat(result).contains("\n2.4;\n");
        assertThat(result).contains("05.00.00");
        assertThat(result).contains("516");
    }

    // --- режим ОМИ (kodMode=2) ---

    @Test
    void build_omiMode_containsKodModeAndParams() {
        ProgramsMode omiMode = buildMode(3L, 2,
                LocalDateTime.of(2026, 4, 30, 6, 0),
                LocalDateTime.of(2026, 4, 30, 6, 12));

        ProgramsModeOmi omi = new ProgramsModeOmi();
        omi.setProgramsMode(omiMode);
        omi.setNumOmi(1);
        omi.setTypeOmi(1);
        omi.setDateNach(LocalDateTime.of(2026, 4, 30, 6, 0));
        omi.setDateCon(LocalDateTime.of(2026, 4, 30, 6, 12));
        omi.setDlit(720);

        when(programsMainRepository.findByNumRpAndNumKa(1, 1525))
                .thenReturn(Optional.of(main));
        when(programsModeRepository.findByNumRpAndNumKaOrderByDateOn(1, 1525))
                .thenReturn(List.of(omiMode));
        when(programsModeOmiRepository.findByProgramsModeId(3L))
                .thenReturn(Optional.of(omi));

        String result = pr01BuilderService.build(1, 1525, 1);

        assertThat(result).contains("\n2.2;\n");
        assertThat(result).contains("06.00.00");
    }

    // --- режим КВД (kodMode=7) ---

    @Test
    void build_kvdMode_containsKodModeAndParams() {
        ProgramsMode kvdMode = buildMode(4L, 7,
                LocalDateTime.of(2026, 4, 30, 7, 0),
                LocalDateTime.of(2026, 4, 30, 7, 25));

        ProgramsModeKvd kvd = new ProgramsModeKvd();
        kvd.setProgramsMode(kvdMode);
        kvd.setPrMsu(1);
        kvd.setPrBssd(1);
        kvd.setPrZg(1);

        when(programsMainRepository.findByNumRpAndNumKa(1, 1525))
                .thenReturn(Optional.of(main));
        when(programsModeRepository.findByNumRpAndNumKaOrderByDateOn(1, 1525))
                .thenReturn(List.of(kvdMode));
        when(programsModeKvdRepository.findByProgramsModeId(4L))
                .thenReturn(Optional.of(kvd));

        String result = pr01BuilderService.build(1, 1525, 1);

        assertThat(result).contains("\n2.7;\n");
        assertThat(result).contains("07.00.00");
        assertThat(result).contains("1,1,1");
    }

    // --- режим 9 фильтруется ---

    @Test
    void build_mode9_isFilteredOut() {
        ProgramsMode mode9 = buildMode(5L, 9,
                LocalDateTime.of(2026, 4, 30, 8, 0),
                LocalDateTime.of(2026, 4, 30, 8, 30));

        when(programsMainRepository.findByNumRpAndNumKa(1, 1525))
                .thenReturn(Optional.of(main));
        when(programsModeRepository.findByNumRpAndNumKaOrderByDateOn(1, 1525))
                .thenReturn(List.of(mode9));

        String result = pr01BuilderService.build(1, 1525, 1);

        assertThat(result).contains("\n1.0;\n"); // 0 режимов после фильтрации
        assertThat(result).doesNotContain("2.9");
    }

    // --- ПРЦА не найдена ---

    @Test
    void build_programNotFound_throwsRuntimeException() {
        when(programsMainRepository.findByNumRpAndNumKa(99, 1525))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pr01BuilderService.build(99, 1525, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ПРЦА не найдена");
    }

    private ProgramsMode buildMode(Long id, int kodMode,
            LocalDateTime dateOn, LocalDateTime dateOff) {
        ProgramsMode mode = new ProgramsMode();
        mode.setId(id);
        mode.setNumRp(1);
        mode.setNumKa(1525);
        mode.setKodMode(kodMode);
        mode.setDateOn(dateOn);
        mode.setDateOff(dateOff);
        mode.setNumPpi(1);
        mode.setDlit(420);
        return mode;
    }

    private ProgramsModeMsu buildMsu(ProgramsMode mode, int tip, int reg, int dlit) {
        ProgramsModeMsu msu = new ProgramsModeMsu();
        msu.setProgramsMode(mode);
        msu.setTip(tip);
        msu.setReg(reg);
        msu.setDlit(dlit);
        msu.setPrMsu1(1);
        msu.setPrVdMsu1(1);
        msu.setPrIkMsu1(1);
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
        msu.setPrVdMsu2(0);
        msu.setPrIkMsu2(0);
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

package ru.laspace.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.laspace.backend.dto.programs.ProgramCreateRequest;
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
import ru.laspace.backend.service.programs.ProgramNumberService;
import ru.laspace.backend.service.programs.ProgramsOnaService;
import ru.laspace.backend.service.programs.impl.ProgramsServiceImpl;

@ExtendWith(MockitoExtension.class)
class ProgramsServiceImplTest {

    @Mock
    private ProgramsMainRepository programsMainRepository;
    @Mock
    private ProgramsModeRepository programsModeRepository;
    @Mock
    private ProgramsModeKvdRepository programsModeKvdRepository;
    @Mock
    private ProgramsModeMsuRepository programsModeMsuRepository;
    @Mock
    private ProgramsModeOmiRepository programsModeOmiRepository;
    @Mock
    private ProgramsModeOnaRepository programsModeOnaRepository;
    @Mock
    private ProgramsOnaService programsOnaService;
    @Mock
    private ProgramNumberService programNumberService;

    @InjectMocks
    private ProgramsServiceImpl programsService;

    @Test
    void saveProgram_noModes_savesMainAndReturnsNumRp() {
        ProgramCreateRequest request = buildRequest(1525, List.of());

        when(programNumberService.generateNextProgramNumber(1525)).thenReturn(1);
        when(programsMainRepository.save(any())).thenAnswer(inv -> {
            ProgramsMain m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        Integer result = programsService.saveProgram(request);

        assertThat(result).isEqualTo(1);
        verify(programsMainRepository).save(any(ProgramsMain.class));
        verify(programsOnaService).saveOnaPrograms(any(), any());
    }

    @Test
    void saveProgram_withMsuMode_savesMsuData() {
        ProgramCreateRequest.MsuData msuData = buildMsuData();
        ProgramCreateRequest.ModeData modeData = buildModeData(1, msuData, null, null, null);
        ProgramCreateRequest request = buildRequest(1525, List.of(modeData));

        when(programNumberService.generateNextProgramNumber(1525)).thenReturn(2);
        when(programsMainRepository.save(any())).thenAnswer(inv -> {
            ProgramsMain m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(programsModeRepository.save(any())).thenAnswer(inv -> {
            ProgramsMode m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        Integer result = programsService.saveProgram(request);

        assertThat(result).isEqualTo(2);
        verify(programsModeMsuRepository).save(any(ProgramsModeMsu.class));
    }

    @Test
    void saveProgram_withKvdMode_savesKvdData() {
        ProgramCreateRequest.KvdData kvdData = new ProgramCreateRequest.KvdData();
        kvdData.setPrMsu(1);
        kvdData.setPrBssd(1);
        kvdData.setPrZg(1);
        ProgramCreateRequest.ModeData modeData = buildModeData(7, null, kvdData, null, null);
        ProgramCreateRequest request = buildRequest(1525, List.of(modeData));

        when(programNumberService.generateNextProgramNumber(1525)).thenReturn(3);
        when(programsMainRepository.save(any())).thenAnswer(inv -> {
            ProgramsMain m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(programsModeRepository.save(any())).thenAnswer(inv -> {
            ProgramsMode m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        programsService.saveProgram(request);

        verify(programsModeKvdRepository).save(any(ProgramsModeKvd.class));
    }

    @Test
    void saveProgram_withOmiMode_savesOmiData() {
        ProgramCreateRequest.OmiData omiData = new ProgramCreateRequest.OmiData();
        omiData.setNumOmi(1);
        omiData.setTypeOmi(1);
        omiData.setDateNach(LocalDateTime.of(2026, 4, 30, 6, 0));
        omiData.setDateCon(LocalDateTime.of(2026, 4, 30, 6, 12));
        omiData.setDlit(720);
        ProgramCreateRequest.ModeData modeData = buildModeData(2, null, null, omiData, null);
        ProgramCreateRequest request = buildRequest(1525, List.of(modeData));

        when(programNumberService.generateNextProgramNumber(1525)).thenReturn(4);
        when(programsMainRepository.save(any())).thenAnswer(inv -> {
            ProgramsMain m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(programsModeRepository.save(any())).thenAnswer(inv -> {
            ProgramsMode m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        programsService.saveProgram(request);

        verify(programsModeOmiRepository).save(any(ProgramsModeOmi.class));
    }

    @Test
    void saveProgram_generatesCorrectNumRp() {
        ProgramCreateRequest request = buildRequest(1525, List.of());

        when(programNumberService.generateNextProgramNumber(1525)).thenReturn(5);
        when(programsMainRepository.save(any())).thenAnswer(inv -> {
            ProgramsMain m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        Integer result = programsService.saveProgram(request);

        assertThat(result).isEqualTo(5);
        verify(programNumberService).generateNextProgramNumber(1525);
    }

    @Test
    void saveProgram_repositoryThrows_propagatesException() {
        ProgramCreateRequest request = buildRequest(1525, List.of());

        when(programNumberService.generateNextProgramNumber(1525)).thenReturn(1);
        when(programsMainRepository.save(any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> programsService.saveProgram(request))
                .isInstanceOf(RuntimeException.class);
    }

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

    private ProgramCreateRequest.ModeData buildModeData(
            int kodMode,
            ProgramCreateRequest.MsuData msuData,
            ProgramCreateRequest.KvdData kvdData,
            ProgramCreateRequest.OmiData omiData,
            ProgramCreateRequest.OnaData onaData) {
        ProgramCreateRequest.ModeData mode = new ProgramCreateRequest.ModeData();
        mode.setNumKa(1525);
        mode.setKodMode(kodMode);
        mode.setDateOn(LocalDateTime.of(2026, 4, 30, 1, 0));
        mode.setDateOff(LocalDateTime.of(2026, 4, 30, 2, 0));
        mode.setNumPpi(1);
        mode.setDlit(420);
        mode.setMsuData(msuData);
        mode.setKvdData(kvdData);
        mode.setOmiData(omiData);
        mode.setOnaData(onaData);
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
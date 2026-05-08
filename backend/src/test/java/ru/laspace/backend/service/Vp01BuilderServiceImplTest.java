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

import ru.laspace.backend.entity.vp.Vp01;
import ru.laspace.backend.entity.vp.Vp01Msu;
import ru.laspace.backend.entity.vp.Vp01Omi;
import ru.laspace.backend.repository.input.FormInRepository;
import ru.laspace.backend.repository.vp.Vp01KvdRepository;
import ru.laspace.backend.repository.vp.Vp01MsuRepository;
import ru.laspace.backend.repository.vp.Vp01OmiRepository;
import ru.laspace.backend.repository.vp.Vp01OnaRepository;
import ru.laspace.backend.repository.vp.Vp01Repository;
import ru.laspace.backend.repository.vp.Vp01TnpRepository;
import ru.laspace.backend.service.vp01.impl.Vp01BuilderServiceImpl;

@ExtendWith(MockitoExtension.class)
class Vp01BuilderServiceImplTest {

    @Mock
    private Vp01Repository vp01Repository;
    @Mock
    private Vp01MsuRepository vp01MsuRepository;
    @Mock
    private Vp01OmiRepository vp01OmiRepository;
    @Mock
    private Vp01OnaRepository vp01OnaRepository;
    @Mock
    private Vp01TnpRepository vp01TnpRepository;
    @Mock
    private Vp01KvdRepository vp01KvdRepository;
    @Mock
    private FormInRepository formInRepository;

    @InjectMocks
    private Vp01BuilderServiceImpl vp01BuilderService;

    private Vp01 vp01;

    @BeforeEach
    void setUp() {
        vp01 = new Vp01();
        vp01.setId(1L);
        vp01.setNumKa(1525);
        vp01.setNumRp(1);
        vp01.setRnf(1);
        vp01.setDtNRp(LocalDateTime.of(2026, 4, 30, 0, 0));
        vp01.setDtKRp(LocalDateTime.of(2026, 4, 30, 23, 59));
    }

    // --- адресная фраза ---

    @Test
    void build_containsCorrectHeader() {
        mockEmptySubrecords();
        when(vp01Repository.findByNumRpAndNumKa(1, 1525)).thenReturn(Optional.of(vp01));
        when(formInRepository.findLatestContentByIdentN(16)).thenReturn(null);

        String result = vp01BuilderService.build(1, 1525, 1);

        assertThat(result).startsWith("ВП01:1525,");
        assertThat(result).contains(":009;");
    }

    // --- НУ04 блок ---

    @Test
    void build_withNu04_includesNu04Content() {
        mockEmptySubrecords();
        when(vp01Repository.findByNumRpAndNumKa(1, 1525)).thenReturn(Optional.of(vp01));
        when(formInRepository.findLatestContentByIdentN(16))
                .thenReturn("НУ04:тестовые данные;\n");

        String result = vp01BuilderService.build(1, 1525, 1);

        assertThat(result).contains("НУ04:тестовые данные;");
    }

    @Test
    void build_withoutNu04_doesNotFail() {
        mockEmptySubrecords();
        when(vp01Repository.findByNumRpAndNumKa(1, 1525)).thenReturn(Optional.of(vp01));
        when(formInRepository.findLatestContentByIdentN(16)).thenReturn(null);

        String result = vp01BuilderService.build(1, 1525, 1);

        assertThat(result).isNotBlank();
    }

    // --- МСУ штатные (tip=1) ---

    @Test
    void build_withMsuStandard_containsArray1() {
        Vp01Msu msu = buildMsu(1L, 1, LocalDateTime.of(2026, 4, 30, 0, 0),
                LocalDateTime.of(2026, 4, 30, 4, 45));

        when(vp01Repository.findByNumRpAndNumKa(1, 1525)).thenReturn(Optional.of(vp01));
        when(vp01MsuRepository.findByVp01IdOrderByNumMsu(1L)).thenReturn(List.of(msu));
        when(vp01OmiRepository.findByVp01IdOrderByNumOmi(1L)).thenReturn(List.of());
        when(vp01OnaRepository.findByVp01IdOrderByNumUstOna(1L)).thenReturn(List.of());
        when(vp01TnpRepository.findByVp01IdOrderByNumTnp(1L)).thenReturn(List.of());
        when(vp01KvdRepository.findByVp01IdOrderByNumKvd(1L)).thenReturn(List.of());
        when(formInRepository.findLatestContentByIdentN(16)).thenReturn(null);

        String result = vp01BuilderService.build(1, 1525, 1);

        assertThat(result).contains("1,1"); // массив 1, 1 запись
        assertThat(result).contains("30.04.2026");
        assertThat(result).contains("00.00.00");
    }

    // --- МСУ технологические (tip=2) ---

    @Test
    void build_withMsuTech_containsArray8() {
        Vp01Msu msuTech = buildMsu(2L, 2, LocalDateTime.of(2026, 4, 30, 10, 0),

                LocalDateTime.of(2026, 4, 30, 14, 30));

        when(vp01Repository.findByNumRpAndNumKa(1, 1525)).thenReturn(Optional.of(vp01));
        when(vp01MsuRepository.findByVp01IdOrderByNumMsu(1L)).thenReturn(List.of(msuTech));
        when(vp01OmiRepository.findByVp01IdOrderByNumOmi(1L)).thenReturn(List.of());
        when(vp01OnaRepository.findByVp01IdOrderByNumUstOna(1L)).thenReturn(List.of());
        when(vp01TnpRepository.findByVp01IdOrderByNumTnp(1L)).thenReturn(List.of());
        when(vp01KvdRepository.findByVp01IdOrderByNumKvd(1L)).thenReturn(List.of());
        when(formInRepository.findLatestContentByIdentN(16)).thenReturn(null);

        String result = vp01BuilderService.build(1, 1525, 1);

        assertThat(result).contains("8,1"); // массив 8, 1 запись
    }

    // --- ОМИ ---

    @Test
    void build_withOmi_containsArray2() {
        Vp01Omi omi = new Vp01Omi();
        omi.setId(1L);
        omi.setNumOmi(1);
        omi.setTypeOmi(1);
        omi.setDateNach(LocalDateTime.of(2026, 4, 30, 6, 0));
        omi.setDateCon(LocalDateTime.of(2026, 4, 30, 6, 12));
        omi.setNumPpi(1);

        when(vp01Repository.findByNumRpAndNumKa(1, 1525)).thenReturn(Optional.of(vp01));
        when(vp01MsuRepository.findByVp01IdOrderByNumMsu(1L)).thenReturn(List.of());
        when(vp01OmiRepository.findByVp01IdOrderByNumOmi(1L)).thenReturn(List.of(omi));
        when(vp01OnaRepository.findByVp01IdOrderByNumUstOna(1L)).thenReturn(List.of());
        when(vp01TnpRepository.findByVp01IdOrderByNumTnp(1L)).thenReturn(List.of());
        when(vp01KvdRepository.findByVp01IdOrderByNumKvd(1L)).thenReturn(List.of());
        when(formInRepository.findLatestContentByIdentN(16)).thenReturn(null);

        String result = vp01BuilderService.build(1, 1525, 1);

        assertThat(result).contains("2,1"); // массив 2, 1 запись
        assertThat(result).contains("06.00.00");
    }

    // --- ВПРЦА не найдена ---

    @Test
    void build_vpNotFound_throwsRuntimeException() {
        when(vp01Repository.findByNumRpAndNumKa(99, 1525)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vp01BuilderService.build(99, 1525, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ВПРЦА не найдена");
    }

    private void mockEmptySubrecords() {
        when(vp01MsuRepository.findByVp01IdOrderByNumMsu(1L)).thenReturn(List.of());
        when(vp01OmiRepository.findByVp01IdOrderByNumOmi(1L)).thenReturn(List.of());
        when(vp01OnaRepository.findByVp01IdOrderByNumUstOna(1L)).thenReturn(List.of());
        when(vp01TnpRepository.findByVp01IdOrderByNumTnp(1L)).thenReturn(List.of());
        when(vp01KvdRepository.findByVp01IdOrderByNumKvd(1L)).thenReturn(List.of());
    }

    private Vp01Msu buildMsu(Long id, int tip, LocalDateTime dateNach, LocalDateTime dateCon) {
        Vp01Msu msu = new Vp01Msu();
        msu.setId(id);
        msu.setTip(tip);
        msu.setDateNach(dateNach);
        msu.setDateCon(dateCon);
        msu.setNumMsu(1);
        msu.setNumPpi(1);
        msu.setComplectMsu1(1);
        msu.setVd11(1);
        msu.setVd12(1);
        msu.setVd13(1);
        msu.setIk14(1);
        msu.setIk15(1);
        msu.setIk16(1);
        msu.setIk17(1);
        msu.setIk18(1);
        msu.setIk19(1);
        msu.setIk110(1);
        msu.setComplectMsu2(0);
        msu.setVd21(0);
        msu.setVd22(0);
        msu.setVd23(0);
        msu.setIk24(0);
        msu.setIk25(0);
        msu.setIk26(0);
        msu.setIk27(0);
        msu.setIk28(0);
        msu.setIk29(0);
        msu.setIk210(0);
        return msu;
    }
}
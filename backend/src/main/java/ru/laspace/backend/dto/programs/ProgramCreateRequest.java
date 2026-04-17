package ru.laspace.backend.dto.programs;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
@EqualsAndHashCode
@ToString
@Schema(description = "Запрос на создание ПРЦА")
public class ProgramCreateRequest {

    @NotNull(message = "Основные данные ПРЦА обязательны")
    @Valid
    @Schema(description = "Основные данные ПРЦА")
    private MainData mainData;

    @NotNull(message = "Список режимов обязателен")
    @Valid
    @Schema(description = "Список режимов")
    private List<ModeData> modes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    @EqualsAndHashCode
    @ToString
    public static class MainData {
        private Long numRp;

        @NotNull(message = "Номер КА обязателен")
        private Long numKa;

        @NotNull(message = "Дата начала обязательна")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateOn;

        @NotNull(message = "Дата окончания обязательна")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateOff;

        @NotNull(message = "Тип рабочей программы обязателен")
        private Integer typeRp;

        private Integer prOtpr;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    @EqualsAndHashCode
    @ToString
    public static class ModeData {
        private Long numRp;
        private Long numKa;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateOn;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateOff;

        private Integer kodMode;
        private Integer numPpi;
        private Integer dlit;
        private String zakazchik;

        private KvdData kvdData;
        private TnpData tnpData;
        private MsuData msuData;
        private OnaData onaData;
        private OmiData omiData;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    @EqualsAndHashCode
    @ToString
    public static class KvdData {
        private Long id;
        private Long idMain;
        private Integer prMsu;
        private Integer prBssd;
        private Integer prZg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    @EqualsAndHashCode
    @ToString
    public static class TnpData {
        private Long id;
        private Long idMain;
        private Integer prMsu;
        private Integer prBssd;
        private Integer prZg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    @EqualsAndHashCode
    @ToString
    public static class MsuData {
        private Long id;
        private Long idMain;
        private Integer tip;
        private Integer reg;
        private Integer dlit;
        private Integer prMsu1;
        private Integer vd1Msu1;
        private Integer vd2Msu1;
        private Integer vd3Msu1;
        private Integer ik4Msu1;
        private Integer ik5Msu1;
        private Integer ik6Msu1;
        private Integer ik7Msu1;
        private Integer ik8Msu1;
        private Integer ik9Msu1;
        private Integer ik10Msu1;
        private Integer prMsu2;
        private Integer vd1Msu2;
        private Integer vd2Msu2;
        private Integer vd3Msu2;
        private Integer ik4Msu2;
        private Integer ik5Msu2;
        private Integer ik6Msu2;
        private Integer ik7Msu2;
        private Integer ik8Msu2;
        private Integer ik9Msu2;
        private Integer ik10Msu2;
        private Integer prBssd;
        private Integer prZg;
        private Integer prOtklZgBssd;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    @EqualsAndHashCode
    @ToString
    public static class OnaData {
        private Long id;
        private Long idMain;
        private Integer typeOmi;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dN;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dK;

        private Integer nOna;
        private Integer nPpi;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    @EqualsAndHashCode
    @ToString
    public static class OmiData {
        private Long id;
        private Long idMain;
        private Integer numOmi;
        private Integer typeOmi;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateNach;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateCon;

        private Integer dlit;
    }
}
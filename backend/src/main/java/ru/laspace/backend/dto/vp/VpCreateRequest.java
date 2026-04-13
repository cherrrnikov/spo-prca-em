package ru.laspace.backend.dto.vp;

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
@Schema(description = "Запрос на создание ВПРЦА")
public class VpCreateRequest {

    @NotNull(message = "Основные данные ВПРЦА обязательны")
    @Valid
    @Schema(description = "Основные данные ВПРЦА")
    private MainData mainData;

    @Schema(description = "Список режимов съёмки (каждый подынтервал отдельно")
    private List<MsuData> msuList;

    @Schema(description = "Список режимов КВД")
    private List<KvdData> kvdList;

    @Schema(description = "Список режимов ТНП")
    private List<TnpData> tnpList;

    @Schema(description = "Список режимов ОМИ")
    private List<OmiData> omiList;

    @Schema(description = "Список режимов юстировки ОНА")
    private List<OnaData> onaList;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    public static class MainData {
        @NotNull(message = "Номер КА обязателен")
        private Integer numKa;

        private Integer numRp;
        private Integer rnf;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dsf;

        @NotNull(message = "Дата начала ПРЦА обязательна")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dtNRp;

        @NotNull(message = "Дата окончания ПРЦА обязательна")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dtKRp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    public static class MsuData {
        private Integer kodReg;
        private Integer numMsu;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateNach;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateCon;

        private Integer complectMsu1;
        private Integer vd11;
        private Integer vd12;
        private Integer vd13;
        private Integer ik14;
        private Integer ik15;
        private Integer ik16;
        private Integer ik17;
        private Integer ik18;
        private Integer ik19;
        private Integer ik110;
        private Integer complectMsu2;
        private Integer vd21;
        private Integer vd22;
        private Integer vd23;
        private Integer ik24;
        private Integer ik25;
        private Integer ik26;
        private Integer ik27;
        private Integer ik28;
        private Integer ik29;
        private Integer ik210;
        private Integer tip;
        private Integer numPpi;
        private Integer dlit;
        private Integer durationCycle;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    public static class KvdData {
        private Integer numKvd;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateNach;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateCon;

        private Integer complectMsu;
        private Integer numPpi;
        private Integer dlit;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    public static class TnpData {
        private Integer numTnp;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateNach;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateCon;

        private Integer numPpi;
        private Integer dlit;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    public static class OmiData {
        private Integer numOmi;
        private Integer typeOmi;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateNach;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateCon;

        private Integer numPpi;
        private Integer dlit;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    public static class OnaData {
        private Integer numUstOna;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateNach;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateCon;

        private Integer numPpi;
        private Integer dlit;
    }
}
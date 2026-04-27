package ru.laspace.backend.dto.input.id02;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@EqualsAndHashCode
@ToString
@Schema(description = "Исходные данные по состоянию бортовых систем КА")
public class Id02Dto {
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Long id;

    @Schema(description = "Регистрационный номер формы обмена", example = "240114001")
    private Integer rnf;

    @Schema(description = "Машинный номер космического аппарата", example = "1")
    private Integer nKa;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Дата начала планируемых суток", example = "2026-01-14")
    private LocalDate dNp;

    @Schema(description = "Сквозной номер планируемых суток", example = "5001")
    private Integer nSp;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Дата составления формы обмена", example = "2026-01-13")
    private LocalDate dsf;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата и время занесения записи в БД", example = "2026-01-14T10:00:00")
    private LocalDateTime dataZap;

    @Schema(description = "Признак исправности МСУ-ГС1 (0/1)", example = "1")
    private Integer i_msu1;

    @Schema(description = "Признак исправности модуля ВД МСУ-ГС1 (0/1)", example = "1")
    private Integer i_vd_1;

    @Schema(description = "Признак исправности модуля ИК МСУ-ГС1 (0/1)", example = "1")
    private Integer i_ik_1;

    @Schema(description = "Признак исправности спектрального канала ВД1 МСУ-ГС1 (0/1)", example = "1")
    private Integer vd1_1;

    @Schema(description = "Признак исправности спектрального канала ВД2 МСУ-ГС1 (0/1)", example = "1")
    private Integer vd2_1;

    @Schema(description = "Признак исправности спектрального канала ВД3 МСУ-ГС1 (0/1)", example = "1")
    private Integer vd3_1;

    @Schema(description = "Признак исправности спектрального канала ИК4 МСУ-ГС1 (0/1)", example = "1")
    private Integer ik4_1;

    @Schema(description = "Признак исправности спектрального канала ИК5 МСУ-ГС1 (0/1)", example = "1")
    private Integer ik5_1;

    @Schema(description = "Признак исправности спектрального канала ИК6 МСУ-ГС1 (0/1)", example = "1")
    private Integer ik6_1;

    @Schema(description = "Признак исправности спектрального канала ИК7 МСУ-ГС1 (0/1)", example = "1")
    private Integer ik7_1;

    @Schema(description = "Признак исправности спектрального канала ИК8 МСУ-ГС1 (0/1)", example = "1")
    private Integer ik8_1;

    @Schema(description = "Признак исправности спектрального канала ИК9 МСУ-ГС1 (0/1)", example = "1")
    private Integer ik9_1;

    @Schema(description = "Признак исправности спектрального канала ИК10 МСУ-ГС1 (0/1)", example = "1")
    private Integer ik10_1;

    @Schema(description = "Признак исправности МСУ-ГС2 (0/1)", example = "1")
    private Integer i_msu2;

    @Schema(description = "Признак исправности модуля ВД МСУ-ГС2 (0/1)", example = "1")
    private Integer i_vd_2;

    @Schema(description = "Признак исправности модуля ИК МСУ-ГС2 (0/1)", example = "1")
    private Integer i_ik_2;

    @Schema(description = "Признак исправности спектрального канала ВД1 МСУ-ГС2 (0/1)", example = "1")
    private Integer vd1_2;

    @Schema(description = "Признак исправности спектрального канала ВД2 МСУ-ГС2 (0/1)", example = "1")
    private Integer vd2_2;

    @Schema(description = "Признак исправности спектрального канала ВД3 МСУ-ГС2 (0/1)", example = "1")
    private Integer vd3_2;

    @Schema(description = "Признак исправности спектрального канала ИК4 МСУ-ГС2 (0/1)", example = "1")
    private Integer ik4_2;

    @Schema(description = "Признак исправности спектрального канала ИК5 МСУ-ГС2 (0/1)", example = "1")
    private Integer ik5_2;

    @Schema(description = "Признак исправности спектрального канала ИК6 МСУ-ГС2 (0/1)", example = "1")
    private Integer ik6_2;

    @Schema(description = "Признак исправности спектрального канала ИК7 МСУ-ГС2 (0/1)", example = "1")
    private Integer ik7_2;

    @Schema(description = "Признак исправности спектрального канала ИК8 МСУ-ГС2 (0/1)", example = "1")
    private Integer ik8_2;

    @Schema(description = "Признак исправности спектрального канала ИК9 МСУ-ГС2 (0/1)", example = "1")
    private Integer ik9_2;

    @Schema(description = "Признак исправности спектрального канала ИК10 МСУ-ГС2 (0/1)", example = "1")
    private Integer ik10_2;

    @Schema(description = "Признак включенного комплекта БССД (0/1)", example = "1")
    private Integer pr_bssd;

    @Schema(description = "Признак исправности БССД1 (0/1)", example = "1")
    private Integer bssd1;

    @Schema(description = "Признак исправности БССД2 (0/1)", example = "1")
    private Integer bssd2;

    @Schema(description = "Признак включения ЗГ (0/1)", example = "1")
    private Integer pr_zg;

    @Schema(description = "Признак отключения ЗГ БССД по окончании съемки (0/1)", example = "1")
    private Integer pr_otkl_zg;
}

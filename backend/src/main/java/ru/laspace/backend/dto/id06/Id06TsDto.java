package ru.laspace.backend.dto.schedule;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Заявка на проведение технологической съемки (id06_ts)")
public class Id06TsDto {

    @Schema(description = "Уникальный идентификатор", example = "1")
    private Long id;

    @Schema(description = "Ссылка на основную запись (id06.id)", example = "1")
    private Long idMain;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата и время начала интервала съемки", example = "2026-01-14T11:00:00")
    private LocalDateTime dn;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата и время конца интервала съемки", example = "2026-01-14T13:00:00")
    private LocalDateTime dk;

    @Schema(description = "Тип съемки", example = "1")
    private Integer tip;

    @Schema(description = "Режим съемки", example = "1")
    private Integer reg;

    @Schema(description = "Признак задействования МСУ-ГС1 (0/1)", example = "1")
    private Integer prMsu1;

    @Schema(description = "Признак проведения съемки в ВД диапазоне МСУ-ГС1 (0/1)", example = "1")
    private Integer prVdMsu1;

    @Schema(description = "Признак проведения съемки в ИК диапазоне МСУ-ГС1 (0/1)", example = "0")
    private Integer prIkMsu1;

    @Schema(description = "Признак задействования спектрального канала ВД1 МСУ-ГС1 (0/1)", example = "1")
    private Integer prVd1_1;

    @Schema(description = "Признак задействования спектрального канала ВД2 МСУ-ГС1 (0/1)", example = "1")
    private Integer prVd2_1;

    @Schema(description = "Признак задействования спектрального канала ВД3 МСУ-ГС1 (0/1)", example = "1")
    private Integer prVd3_1;

    @Schema(description = "Признак задействования спектрального канала ИК4 МСУ-ГС1 (0/1)", example = "0")
    private Integer prIk4_1;

    @Schema(description = "Признак задействования спектрального канала ИК5 МСУ-ГС1 (0/1)", example = "0")
    private Integer prIk5_1;

    @Schema(description = "Признак задействования спектрального канала ИК6 МСУ-ГС1 (0/1)", example = "0")
    private Integer prIk6_1;

    @Schema(description = "Признак задействования спектрального канала ИК7 МСУ-ГС1 (0/1)", example = "0")
    private Integer prIk7_1;

    @Schema(description = "Признак задействования спектрального канала ИК8 МСУ-ГС1 (0/1)", example = "0")
    private Integer prIk8_1;

    @Schema(description = "Признак задействования спектрального канала ИК9 МСУ-ГС1 (0/1)", example = "0")
    private Integer prIk9_1;

    @Schema(description = "Признак задействования спектрального канала ИК10 МСУ-ГС1 (0/1)", example = "0")
    private Integer prIk10_1;

    @Schema(description = "Признак задействования МСУ-ГС2 (0/1)", example = "0")
    private Integer prMsu2;

    @Schema(description = "Признак проведения съемки в ВД диапазоне МСУ-ГС2 (0/1)", example = "0")
    private Integer prVdMsu2;

    @Schema(description = "Признак проведения съемки в ИК диапазоне МСУ-ГС2 (0/1)", example = "0")
    private Integer prIkMsu2;

    @Schema(description = "Признак задействования спектрального канала ВД1 МСУ-ГС2 (0/1)", example = "0")
    private Integer prVd1_2;

    @Schema(description = "Признак задействования спектрального канала ВД2 МСУ-ГС2 (0/1)", example = "0")
    private Integer prVd2_2;

    @Schema(description = "Признак задействования спектрального канала ВД3 МСУ-ГС2 (0/1)", example = "0")
    private Integer prVd3_2;

    @Schema(description = "Признак задействования спектрального канала ИК4 МСУ-ГС2 (0/1)", example = "0")
    private Integer prIk4_2;

    @Schema(description = "Признак задействования спектрального канала ИК5 МСУ-ГС2 (0/1)", example = "0")
    private Integer prIk5_2;

    @Schema(description = "Признак задействования спектрального канала ИК6 МСУ-ГС2 (0/1)", example = "0")
    private Integer prIk6_2;

    @Schema(description = "Признак задействования спектрального канала ИК7 МСУ-ГС2 (0/1)", example = "0")
    private Integer prIk7_2;

    @Schema(description = "Признак задействования спектрального канала ИК8 МСУ-ГС2 (0/1)", example = "0")
    private Integer prIk8_2;

    @Schema(description = "Признак задействования спектрального канала ИК9 МСУ-ГС2 (0/1)", example = "0")
    private Integer prIk9_2;

    @Schema(description = "Признак задействования спектрального канала ИК10 МСУ-ГС2 (0/1)", example = "0")
    private Integer prIk10_2;

    @Schema(description = "Признак отключения ЗГ БССД по окончании съемки (0/1)", example = "0")
    private Integer prOtklZg;
}
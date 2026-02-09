package ru.laspace.backend.dto.ro02;

import java.time.LocalDate;

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
@Schema(description = "Запись сезонного разворота из таблицы ro_02")
public class Ro02Dto {

    @Schema(description = "Уникальный идентификатор", example = "1")
    private Long id;

    @Schema(description = "Регистрационный номер формы обмена", example = "240114001")
    private Integer rnf;

    @Schema(description = "Машинный номер космического аппарата", example = "1")
    private Integer nKa;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Дата составления формы обмена", example = "2026-01-13")
    private LocalDate dsf;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Дата начала подготовительных работ к развороту", example = "2026-01-14")
    private LocalDate dataN;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Дата разворота", example = "2026-01-15")
    private LocalDate dataRazv;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Дата окончания работ (опционально)", example = "2026-01-16")
    private LocalDate dataK;

    @Schema(description = "Номер записи в таблице Form Inp", example = "101")
    private Integer nFormId;
}
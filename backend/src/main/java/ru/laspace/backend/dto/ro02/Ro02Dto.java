package ru.laspace.backend.dto.ro02;

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
@Schema(description = "Запись сезонного разворота из таблицы ro_02")
public class Ro02Dto {

    @Schema(description = "Уникальный идентификатор", example = "1")
    private Long id;

    @Schema(description = "Регистрационный номер формы обмена", example = "240114001")
    private Integer rnf;

    @Schema(description = "Машинный номер космического аппарата", example = "1")
    private Integer nKa;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата составления формы обмена", example = "2026-01-14T11:30:00")
    private LocalDateTime dsf;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата начала подготовительных работ к развороту", example = "2026-01-14T11:30:00")
    private LocalDateTime dataN;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата разворота", example = "2026-01-14T11:30:00")
    private LocalDateTime dataRazv;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата окончания работ (опционально)", example = "2026-01-14T11:30:00")
    private LocalDateTime dataK;

    @Schema(description = "Номер записи в таблице Form Inp", example = "101")
    private Integer nFormId;
}
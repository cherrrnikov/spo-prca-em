package ru.laspace.backend.dto.id;

import java.time.LocalDate;
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
@Schema(description = "Основная запись заявки из таблицы id06")
public class Id06MainDto {

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

    @Schema(description = "Количество заявок на режимы работы ЦА", example = "3")
    private Integer kZajv;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата и время занесения записи в БД", example = "2026-01-14T10:00:00")
    private LocalDateTime dataZap;

    @Schema(description = "Номер записи в таблице Form Inp", example = "101")
    private Integer nFormId;

    @Schema(description = "Признак обработки (0 - не обработано, 1 - обработано)", example = "0")
    private Integer used;
}
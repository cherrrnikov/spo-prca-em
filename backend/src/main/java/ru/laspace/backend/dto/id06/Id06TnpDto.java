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
@Schema(description = "Заявка на проведение режима ТНП (id06_tnp)")
public class Id06TnpDto {

    @Schema(description = "Уникальный идентификатор", example = "1")
    private Long id;

    @Schema(description = "Ссылка на основную запись (id06.id)", example = "1")
    private Long idMain;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата и время начала интервала ТНП", example = "2026-01-14T16:00:00")
    private LocalDateTime dn;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата и время конца интервала ТНП", example = "2026-01-14T16:05:00")
    private LocalDateTime dk;

    @Schema(description = "Длительность передачи тестового сигнала (секунды)", example = "300")
    private Integer dlit;
}
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
@Schema(description = "Заявка на проведение калибровки ВД (id06_kvd)")
public class Id06KvdDto {

    @Schema(description = "Уникальный идентификатор", example = "1")
    private Long id;

    @Schema(description = "Ссылка на основную запись (id06.id)", example = "1")
    private Long idMain;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата и время начала интервала калибровки", example = "2026-01-14T09:00:00")
    private LocalDateTime dn;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата и время конца интервала калибровки", example = "2026-01-14T10:30:00")
    private LocalDateTime dk;

    @Schema(description = "Признак задействования комплектов МСУ-ГС (0/1)", example = "1")
    private Integer prMsu;

    @Schema(description = "Признак включенного комплекта БССД (0/1)", example = "1")
    private Integer prBssd;

    @Schema(description = "Признак включения ЗГ (0/1)", example = "0")
    private Integer prZg;
}
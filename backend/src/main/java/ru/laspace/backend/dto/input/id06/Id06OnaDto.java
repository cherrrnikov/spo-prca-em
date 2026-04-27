package ru.laspace.backend.dto.input.id06;

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
@Schema(description = "Заявка на проведение юстировки ОНА (id06_ona)")
public class Id06OnaDto {

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

    @Schema(description = "Номер юстируемой антенны", example = "1")
    private Integer nOna;

    @Schema(description = "Длительность юстировки", example = "300")
    private Integer dlit;
}
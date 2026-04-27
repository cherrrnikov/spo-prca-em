package ru.laspace.backend.dto.input.kr01;

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
@Schema(description = "Основная запись коррекции орбиты из таблицы kr01_main")
public class Kr01MainDto {

    @Schema(description = "Уникальный идентификатор", example = "1")
    private Long id;

    @Schema(description = "Регистрационный номер формы обмена", example = "240114001")
    private Integer rnf;

    @Schema(description = "Машинный номер космического аппарата", example = "1")
    private Integer nKa;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата составления формы обмена", example = "2026-01-14T10:00:00")
    private LocalDateTime dsf;

    @Schema(description = "Номер баллистического центра: 1-СГК, 2-ЦУП, 3-другой", example = "1")
    private Integer nBc;

    @Schema(description = "Номер задания", example = "1001")
    private Integer nZad;

    @Schema(description = "Количество импульсов коррекции", example = "3")
    private Integer kImp;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата и время распаковки ФО", example = "2026-01-14T10:05:00")
    private LocalDateTime dtZap;

    @Schema(description = "Номер записи в таблице Form Inp", example = "101")
    private Integer nFormId;

    @Schema(description = "Признак обработки (0 - не обработано, 1 - обработано)", example = "0")
    private Integer used;
}

package ru.laspace.backend.dto.kr01;

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
@Schema(description = "Импульс коррекции орбиты из таблицы kr01")
public class Kr01ImpulseDto {

    @Schema(description = "Уникальный идентификатор", example = "1")
    private Long id;

    @Schema(description = "Ссылка на основную запись (kr01_main.id)", example = "1")
    private Long idMain;

    @Schema(description = "Номер витка приложения импульса", example = "1250")
    private Integer nVit;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата и время приложения импульса", example = "2026-01-14T11:30:00")
    private LocalDateTime dateIm;

    @Schema(description = "Длительность приложения импульса, сек", example = "120")
    private Integer dlit;

    @Schema(description = "Признак системы ориентации: 0-скоростная инерциальная, 1-скоростная с программным разворотом", example = "1")
    private Short prOr;

    @Schema(description = "Угловая скорость программного разворота по тангажу, град/сек", example = "0.5")
    private Double uglV;

    @Schema(description = "Масса КА на момент включения двигательной установки, кг", example = "1500.5")
    private Double massa;

    @Schema(description = "Условный номер двигательной установки: 1-4 ВКИ1, 5-6 ВКИ2", example = "3")
    private Integer nDu;

    @Schema(description = "Признак варианта расчета: 0-предварительный, 1-основной", example = "1")
    private Short prVar;
}
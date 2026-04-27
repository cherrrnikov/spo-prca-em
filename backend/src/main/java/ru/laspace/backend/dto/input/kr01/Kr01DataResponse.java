package ru.laspace.backend.dto.input.kr01;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
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
@Schema(description = "Полный набор данных для коррекции орбиты (ВКИ)")

public class Kr01DataResponse {
    @Schema(description = "Основная запись коррекции орбиты")
    private Kr01MainDto main;

    @Schema(description = "Список импульсов коррекции")
    private List<Kr01ImpulseDto> impulses;

    @Schema(description = "Общее количество импульсов", hidden = true)
    public int getTotalImpulses() {
        return impulses != null ? impulses.size() : 0;
    }
}

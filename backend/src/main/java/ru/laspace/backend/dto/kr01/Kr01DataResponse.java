package ru.laspace.backend.dto.kr01;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

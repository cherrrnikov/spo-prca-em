package ru.laspace.backend.dto.ro02;

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
@Schema(description = "Данные сезонного разворота КА")
public class Ro02DataResponse {
    @Schema(description = "Список записей сезонных разворотов")
    private List<Ro02Dto> rotations;

    @Schema(description = "Общее количество записей", hidden = true)
    public int getTotalRotations() {
        return rotations != null ? rotations.size() : 0;
    }
}

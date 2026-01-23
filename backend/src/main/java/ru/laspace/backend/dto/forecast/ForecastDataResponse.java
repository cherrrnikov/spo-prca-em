package ru.laspace.backend.dto.forecast;

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
public class ForecastDataResponse {
    @Schema(description = "Прогнозные данные (тени и засветки)")
    private ForecastDto forecast;

    @Schema(description = "Список теней")
    private List<ShadowDto> shadows;

    @Schema(description = "Список засветок")
    private List<ZasvetkaDto> zasvetki;
}

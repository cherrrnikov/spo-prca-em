package ru.laspace.backend.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.input.forecast.ForecastDataResponse;
import ru.laspace.backend.service.input.ForecastService;

@Slf4j
@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
@Tag(name = "Прогнозные данные", description = "API для работы с прогнозными данными (тени, засветки)")
public class ForecastController {
        private final ForecastService forecastService;

        @Operation(summary = "Получить прогнозные данные", description = """
                        Возвращает прогнозные данные (тени и засветки) для указанного космического аппарата на дату.
                        """)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Прогнозные данные успешно найдены", content = @Content(schema = @Schema(implementation = ForecastDataResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Прогнозные данные не найдены", content = @Content(schema = @Schema(implementation = Void.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = Void.class)))
        })
        @GetMapping("/operator/{date}")
        public ResponseEntity<ForecastDataResponse> getForecastData(
                        @Parameter(description = "Дата планирования в формате YYYY-MM-DD", example = "2026-01-14", required = true) @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

                log.info("Запрос прогнозных данных на дату: {}", date);

                ForecastDataResponse data = forecastService.getOperatorData(date);

                if (data == null) {
                        log.info("Прогнозные данные не найдены на дату: {}", date);
                        return ResponseEntity.notFound().build();
                }

                log.info("Найдено теней: {}, засветок: {}",
                                data.getShadows().size(), data.getZasvetki().size());
                return ResponseEntity.ok(data);
        }
}

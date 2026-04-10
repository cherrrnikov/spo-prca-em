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
import ru.laspace.backend.dto.ro02.Ro02DataResponse;
import ru.laspace.backend.service.Ro02Service;

@Slf4j
@RestController
@RequestMapping("/api/rotation")
@RequiredArgsConstructor
@Tag(name = "Сезонные развороты", description = "API для работы с данными сезонных разворотов космических аппаратов")
public class Ro02Controller {

    private final Ro02Service ro02Service;

    @Operation(summary = "Получить данные сезонных разворотов", description = """
            Возвращает данные сезонных разворотов для указанной даты.
            Ищет записи, где дата попадает в диапазон от data_n до data_k (или data_razv).
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные сезонных разворотов успешно найдены", content = @Content(schema = @Schema(implementation = Ro02DataResponse.class))),
            @ApiResponse(responseCode = "404", description = "Данные сезонных разворотов не найдены", content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @GetMapping("/seasonal/{date}")
    public ResponseEntity<Ro02DataResponse> getRo02Data(
            @Parameter(description = "Дата планирования в формате YYYY-MM-DD", example = "2026-01-14", required = true) @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Запрос данных сезонных разворотов на дату: {}", date);

        try {
            Ro02DataResponse data = ro02Service.getRo02Data(date);

            if (data == null) {
                log.info("Данные сезонных разворотов не найдены на дату: {}", date);
                return ResponseEntity.notFound().build();
            }

            log.info("Найдено {} записей сезонных разворотов для даты {}",
                    data.getTotalRotations(), date);
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            log.error("Ошибка при получении данных сезонных разворотов на дату {}: {}",
                    date, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Проверка работоспособности API разворотов", description = "Проверяет, что сервис сезонных разворотов работает")
    @ApiResponse(responseCode = "200", description = "Сервис работает", content = @Content(schema = @Schema(example = "Rotation API работает, время: 2026-01-14T10:00:00")))
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Rotation API работает, время: " + java.time.LocalDateTime.now());
    }
}
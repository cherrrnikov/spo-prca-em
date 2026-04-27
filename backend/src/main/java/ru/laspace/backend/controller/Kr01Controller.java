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
import ru.laspace.backend.dto.input.kr01.Kr01DataResponse;
import ru.laspace.backend.service.input.Kr01Service;

@Slf4j
@RestController
@RequestMapping("/api/vki")
@RequiredArgsConstructor
@Tag(name = "Коррекция орбиты (ВКИ)", description = "API для работы с данными коррекции орбиты космических аппаратов")
public class Kr01Controller {

    private final Kr01Service kr01Service;

    @Operation(summary = "Получить данные коррекции орбиты (ВКИ)", description = """
            Возвращает данные коррекции орбиты (ВКИ) для указанной даты.
            Ищет последнюю запись по дате приложения импульса (date_im).
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные коррекции орбиты успешно найдены", content = @Content(schema = @Schema(implementation = Kr01DataResponse.class))),
            @ApiResponse(responseCode = "404", description = "Данные коррекции орбиты не найдены", content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @GetMapping("/correction/{date}")
    public ResponseEntity<Kr01DataResponse> getKr01Data(
            @Parameter(description = "Дата приложения импульса в формате YYYY-MM-DD", example = "2026-01-14", required = true) @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Запрос данных коррекции орбиты (ВКИ) на дату: {}", date);

        Kr01DataResponse data = kr01Service.getKr01Data(date);

        if (data == null) {
            log.info("Данные коррекции орбиты не найдены на дату: {}", date);
            return ResponseEntity.notFound().build();
        }

        log.info("Найдено {} импульсов коррекции для даты {}",
                data.getTotalImpulses(), date);
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Проверка работоспособности API ВКИ", description = "Проверяет, что сервис коррекции орбиты работает")
    @ApiResponse(responseCode = "200", description = "Сервис работает", content = @Content(schema = @Schema(example = "VKI API работает, время: 2026-01-14T10:00:00")))
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("VKI API работает, время: " + java.time.LocalDateTime.now());
    }
}
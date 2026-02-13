package ru.laspace.backend.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import ru.laspace.backend.dto.id06.Id06DataResponse;
import ru.laspace.backend.service.ConstantsService;
import ru.laspace.backend.service.Id06Service;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedule")
@CrossOrigin
@Tag(name = "Планирование ПРЦА", description = "API для работы с данными планирования программ работы целевой аппаратуры")
public class Id06Controller {

    private final Id06Service id06Service;
    private final ConstantsService constantsService;

    @Operation(summary = "Получить данные оператора по дате", description = """
            Возвращает последнюю запись из таблицы id06 и все связанные записи
            (калибровка ВД, режимы ТНП, технологические съемки) для указанной даты планирования.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные успешно найдены", content = @Content(schema = @Schema(implementation = Id06DataResponse.class))),
            @ApiResponse(responseCode = "404", description = "Данные не найдены для указанной даты", content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @GetMapping("/operator/{date}")
    public ResponseEntity<Id06DataResponse> getOperatorData(
            @Parameter(description = "Дата планирования в формате YYYY-MM-DD", example = "2026-01-14", required = true) @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Запрос данных оператора для даты: {}", date);

        try {
            Id06DataResponse data = (Id06DataResponse) id06Service.getOperatorData(date);

            if (data == null) {
                log.info("Данные не найдены для даты: {}", date);
                return ResponseEntity.notFound().build();
            }

            log.info("Найдено {} интервалов для даты {}", data.getTotalIntervals(), date);
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            log.error("Ошибка при получении данных для даты {}: {}", date, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Получить стандартные длительности режимов", description = "Возвращает стандартные длительности в секундах для всех типов режимов из таблицы констант")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Длительности успешно получены", content = @Content(schema = @Schema(example = """
                       {
                           "astr": 300,
                           "s": 420,
                           "omi": 60,
                           "tnp": 516,
                           "kvd": 420,
                           "ts": 420,
                           "ona": 60
                       }
                    """))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/mode-durations")
    public ResponseEntity<Map<String, Integer>> getModeDurations() {
        log.info("Запрос стандартных длительностей режимов");

        try {
            Map<String, Integer> durations = constantsService.getModeDurations();
            log.info("Возвращаем {} длительностей режимов", durations.size());
            return ResponseEntity.ok(durations);

        } catch (Exception e) {
            log.error("Ошибка при получении длительностей режимов: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Проверка работоспособности API", description = "Проверяет, что сервис планирования работает")
    @ApiResponse(responseCode = "200", description = "Сервис работает", content = @Content(schema = @Schema(example = "Schedule API работает, время: 2026-01-14T10:00:00")))
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Schedule API работает, время: " + java.time.LocalDateTime.now());
    }
}
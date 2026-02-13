package ru.laspace.backend.controller;

import java.time.LocalDate;

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
import ru.laspace.backend.dto.id02.Id02Dto;
import ru.laspace.backend.service.Id02Service;

@Slf4j
@RestController
@RequestMapping("/api/bort")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Состояние бортовых систем", description = "API для получения данных о состоянии бортовых систем КА")
public class Id02Controller {

    private final Id02Service id02Service;

    @Operation(summary = "Получить состояние бортовых систем по дате", description = """
            Возвращает последнюю запись из таблицы id02 с данными о состоянии бортовых систем
            (исправность МСУ-ГС1/МСУ-ГС2, спектральных каналов, БССД, ЗГ) для указанной даты планирования.
            Данные используются для определения возможности проведения съемки в различных режимах.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные успешно найдены", content = @Content(schema = @Schema(implementation = Id02Dto.class))),
            @ApiResponse(responseCode = "404", description = "Данные не найдены для указанной даты", content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @GetMapping("/{date}")
    public ResponseEntity<Id02Dto> getBortData(
            @Parameter(description = "Дата планирования в формате YYYY-MM-DD", example = "2026-01-14", required = true) @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Запрос состояния бортовых систем для даты: {}", date);

        try {
            Id02Dto data = id02Service.getBortData(date);

            if (data == null) {
                log.info("Данные о состоянии бортовых систем не найдены для даты: {}", date);
                return ResponseEntity.notFound().build();
            }

            log.info("Найдена запись ИД02 ID={}, КА={}, RNF={} для даты {}",
                    data.getId(), data.getNKa(), data.getRnf(), date);

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            log.error("Ошибка при получении данных ИД02 для даты {}: {}",
                    date, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Проверка работоспособности API ИД02", description = "Проверяет, что сервис данных о состоянии бортовых систем работает")
    @ApiResponse(responseCode = "200", description = "Сервис работает", content = @Content(schema = @Schema(example = "ID02 API работает, время: 2026-01-14T10:00:00")))
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("ID02 API работает, время: " + java.time.LocalDateTime.now());
    }
}
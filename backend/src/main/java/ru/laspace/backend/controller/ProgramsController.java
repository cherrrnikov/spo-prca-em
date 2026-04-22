package ru.laspace.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.programs.ProgramCreateRequest;
import ru.laspace.backend.service.Pr01Service;
import ru.laspace.backend.service.Pr03Service;
import ru.laspace.backend.service.Pr04Service;
import ru.laspace.backend.service.ProgramsService;

@Slf4j
@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
@Tag(name = "Программа работы целевой аппаратуры", description = "API для сохранения ПРЦА")
public class ProgramsController {
    private final ProgramsService programsService;
    private final Pr01Service pr01Service;
    private final Pr03Service pr03Service;
    private final Pr04Service pr04Service;

    @Operation(summary = "Создать ПРЦА", description = "Сохраняет ПРЦА со всеми режимами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "ПРЦА успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/create")
    public ResponseEntity<Map<String, Long>> createProgram(@Valid @RequestBody ProgramCreateRequest request) {
        log.info("=== Получен запрос на создание ПРЦА ===");

        log.info("Номер ПРЦА: {}, Номер КА: {}",
                request.getMainData().getNumRp(),
                request.getMainData().getNumKa());
        log.info("Количество режимов в запросе: {}", request.getModes() != null ? request.getModes().size() : 0);

        Long generatedNumRp = programsService.saveProgram(request);
        log.info("ПРЦА успешно сохранена, присвоен номер: {}", generatedNumRp);

        Map<String, Long> response = new HashMap<>();
        response.put("numRp", generatedNumRp);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Сформировать ПР01", description = "Формирует и сохраняет форму обмена ПР01 по номеру ПРЦА")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ПР01 успешно сформирована"),
            @ApiResponse(responseCode = "404", description = "ПРЦА не найдена", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/{numRp}/{numKa}/pr01/generate")
    public ResponseEntity<String> generatePr01(
            @PathVariable Long numRp,
            @PathVariable Long numKa) {
        log.info("=== Получен запрос на формирование ПР01: numRp={}, numKa={} ===", numRp, numKa);
        String fo = pr01Service.generateAndSave(numRp, numKa);
        return ResponseEntity.ok(fo);
    }

    @Operation(summary = "Сформировать и сохранить ПР03")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ПР03 успешно сформирована"),
            @ApiResponse(responseCode = "404", description = "ПРЦА не найдена", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/{numRp}/{numKa}/pr03/generate")
    public ResponseEntity<String> generatePr03(
            @PathVariable Long numRp,
            @PathVariable Long numKa) {
        log.info("=== Получен запрос на формирование ПР03: numRp={}, numKa={} ===", numRp, numKa);
        String fo = pr03Service.generateAndSave(numRp, numKa);
        return ResponseEntity.ok(fo);
    }

    @Operation(summary = "Сформировать и сохранить ПР04")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ПР04 успешно сформирована"),
            @ApiResponse(responseCode = "404", description = "ПРЦА не найдена", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/{numRp}/{numKa}/pr04/generate")
    public ResponseEntity<String> generatePr04(
            @PathVariable Long numRp,
            @PathVariable Long numKa) {
        log.info("=== Получен запрос на формирование ПР04: numRp={}, numKa={} ===", numRp, numKa);
        String fo = pr04Service.generateAndSave(numRp, numKa);
        return ResponseEntity.ok(fo);
    }
}
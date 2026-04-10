package ru.laspace.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.programs.ProgramCreateRequest;
import ru.laspace.backend.service.ProgramsService;

@Slf4j
@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
@Tag(name = "Программа работы целевой аппаратуры", description = "API для сохранения ПРЦА")
public class ProgramsController {
    private final ProgramsService programsService;

    @Operation(summary = "Создать ПРЦА", description = "Сохраняет ПРЦА со всеми режимами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "ПРЦА успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/create")
    public ResponseEntity<Map<String, Long>> createProgram(@RequestBody ProgramCreateRequest request) {
        log.info("=== Получен запрос на создание ПРЦА ===");

        if (request == null) {
            log.error("request is null");
            return ResponseEntity.badRequest().build();
        }

        if (request.getMainData() == null) {
            log.error("mainData is null! Request: {}", request);
            return ResponseEntity.badRequest().build();
        }

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
}
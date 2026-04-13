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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.vp.VpCreateRequest;
import ru.laspace.backend.service.VpService;

@Slf4j
@RestController
@RequestMapping("/api/vp")
@RequiredArgsConstructor
@Tag(name = "Выписка из программы работы ЦА", description = "API для сохранения ВПРЦА")
public class VpController {
    private final VpService vpService;

    @Operation(summary = "Создать ВПРЦА", description = "Сохраняет ВПРЦА со всеми режимами (каждый подынтервал отдельной записью")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "ВПРЦА успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/create")
    public ResponseEntity<Map<String, Long>> createVp(@Valid @RequestBody VpCreateRequest request) {
        log.info("=== Получен запрос на создание ВПРЦА ===");
        log.info("Номер РП: {}, Номер КА: {}", request.getMainData().getNumRp(), request.getMainData().getNumKa());

        Long vpId = vpService.saveVp(request);
        log.info("ВПРЦА успешно сохранена, id: {}", vpId);

        Map<String, Long> response = new HashMap<>();
        response.put("vpId", vpId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
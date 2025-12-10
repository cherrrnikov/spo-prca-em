package ru.laspace.spo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.dto.request.LoginRequest;
import ru.laspace.spo.dto.request.RefreshTokenRequest;
import ru.laspace.spo.dto.response.JwtResponse;
import ru.laspace.spo.service.AuthService;

@Slf4j
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "API для входа, выхода и обновления токенов ")
public class AuthController {
        private final AuthService authService;

        @Operation(summary = "Вход в систему", description = "Аутентификация пользователя и получение JWT токенов")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Успешная аутентификация", content = @Content(schema = @Schema(implementation = JwtResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
                        @ApiResponse(responseCode = "401", description = "Неверный логин или пароль"),
                        @ApiResponse(responseCode = "403", description = "Аккаунт отключен"),
        })
        @PostMapping("/login")
        public ResponseEntity<JwtResponse> login(
                        @Parameter(description = "Данные для входа", required = true) @Valid @RequestBody LoginRequest request) {
                log.info("Запрос на логин: {}", request.getUsername());
                JwtResponse response = authService.login(request);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Обновление токенов", description = "Получение новой пары access/refresh токенов по старому refreshToken")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Токены успешно обновлены", content = @Content(schema = @Schema(implementation = JwtResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Неверный формат токена"),
                        @ApiResponse(responseCode = "401", description = "RefreshToken недействителен"),
                        @ApiResponse(responseCode = "403", description = "RefreshToken отозван или просрочен"),
        })
        @PostMapping("/refresh")
        public ResponseEntity<JwtResponse> refresh(
                        @Parameter(description = "RefreshToken", required = true) @Valid @RequestBody RefreshTokenRequest request) {
                log.info("Запрос на обновление токена");
                JwtResponse response = authService.refreshToken(request);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Выход из системы", description = "Завершение сессии и отзыв refresh token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Успешный выход"),
                        @ApiResponse(responseCode = "400", description = "Refresh token обязателен"),
                        @ApiResponse(responseCode = "401", description = "Refresh token отозван или истек"),
        })
        @PostMapping("/logout")
        public ResponseEntity<Void> logout(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Refresh token для отзыва", required = true, content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))) @Valid @RequestBody RefreshTokenRequest request) {

                authService.logout(request.getRefreshToken());
                return ResponseEntity.ok().build();
        }

        @Operation(summary = "Проверка токена", description = "Проверка валидности JWT")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Токен валиден"),
                        @ApiResponse(responseCode = "401", description = "Токен недействителен"),
        })
        @GetMapping("/validate")
        public ResponseEntity<Void> validateToken() {
                return ResponseEntity.ok().build();
        }
}

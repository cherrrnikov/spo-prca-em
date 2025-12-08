package ru.laspace.spo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
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
import ru.laspace.spo.security.UserDetailsImpl;
import ru.laspace.spo.service.AuthService;

@Slf4j
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "API для входа, выхода и обновления токенов ")
public class AuthController {
    private AuthService authService;

    @Operation(summary = "Вход в систему", description = "Аутентификация пользователя и получение JWT токенов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация", content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "401", description = "Неверный логин или пароль"),
            @ApiResponse(responseCode = "403", description = "Аккаунт отключен"),
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Запрос на логин: {}", request.getUsername());
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Запрос на обновление токена");
        JwtResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String refreshToken = authHeader.substring(7);
            authService.logout(refreshToken, userDetails.getId());
        }

        return ResponseEntity.ok().build();
    }
}

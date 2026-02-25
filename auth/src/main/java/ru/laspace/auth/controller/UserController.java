package ru.laspace.auth.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.dto.request.CreateUserRequest;
import ru.laspace.auth.dto.response.UserResponse;
import ru.laspace.auth.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Управление пользователями", description = "API для работы с пользователями")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Текущий пользователь", description = "Получение информации о текущем аутентифицированном пользователе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о пользователе"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        log.info("GET /api/users/me");
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @Operation(summary = "Создать пользователя", description = "Создание нового пользователя (требуется роль ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "Данные для создания пользователя", required = true) @Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/users - creating user: {}", request.getUsername());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Все пользователи", description = "Получение списка всех пользователей (требуется роль ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /api/users - getting all users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Получить пользователя по ID", description = "Получение информации о пользователе по ID (требуется роль ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.info("GET /api/users/{}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Получить пользователя по имени", description = "Получение информации о пользователе по username (требуется роль ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserByUsername(
            @Parameter(description = "Имя пользователя", required = true) @PathVariable String username) {
        log.info("GET /api/users/by-username/{}", username);
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @Operation(summary = "Обновить роли", description = "Обновление ролей пользователя (требуется роль ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Роли обновлены"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRoles(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id,
            @Parameter(description = "Новый набор ролей", required = true) @RequestBody Set<String> roles) {
        log.info("PUT /api/users/{}/roles with roles: {}", id, roles);
        return ResponseEntity.ok(userService.updateUserRoles(id, roles));
    }

    @Operation(summary = "Удалить пользователя", description = "Удаление пользователя (требуется роль ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь удален"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.info("DELETE /api/users/{}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Заблокировать пользователя", description = "Блокировка доступа пользователя (требуется роль ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь заблокирован"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> disableUser(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.info("POST /api/users/{}/disable", id);
        return ResponseEntity.ok(userService.disableUser(id));
    }

    @Operation(summary = "Разблокировать пользователя", description = "Разблокировка доступа пользователя (требуется роль ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь разблокирован"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> enableUser(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.info("POST /api/users/{}/enable", id);
        return ResponseEntity.ok(userService.enableUser(id));
    }

    @Operation(summary = "Разблокировать аккаунт", description = "Снятие блокировки аккаунта после неудачных попыток (требуется роль ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт разблокирован"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> unlockUserAccount(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.info("POST /api/users/{}/unlock", id);
        return ResponseEntity.ok(userService.unlockUserAccount(id));
    }

    @Operation(summary = "Сбросить пароль", description = "Принудительный сброс пароля пользователя (требуется роль ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пароль сброшен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "400", description = "Неверный новый пароль")
    })
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> resetUserPassword(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON с новым паролем", required = true, content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Пример запроса", value = "{\"newPassword\": \"NewSecurePassword123!\"}"))) @RequestBody Map<String, String> request) {

        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password is required");
        }

        log.info("POST /api/users/{}/reset-password", id);
        return ResponseEntity.ok(userService.resetUserPassword(id, newPassword));
    }
}
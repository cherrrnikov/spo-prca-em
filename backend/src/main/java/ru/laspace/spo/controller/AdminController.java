package ru.laspace.spo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.dto.request.CreateUserRequest;
import ru.laspace.spo.dto.request.UpdateUserRolesRequest;
import ru.laspace.spo.dto.response.UserResponse;
import ru.laspace.spo.service.AdminService;

@Slf4j
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Администрирование пользователей", description = "API для управления пользователями (требуется роль ADMIN)")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Создать пользователя", description = "Создание нового пользователя с указанием ролей. Пароль автоматически хэшируется.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "Данные для создания пользователя", required = true) @Valid @RequestBody CreateUserRequest request) {
        log.info("Создание пользователя: {}", request.getUsername());
        UserResponse response = adminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Получить всех пользователей", description = "Получение списка всех пользователей системы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    })
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Получение списка всех пользователей");
        List<UserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Получить пользователя по ID", description = "Получение информации о пользователе по его ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.info("Получение пользователя по ID: {}", id);
        UserResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Обновить роли пользователя", description = "Полная замена ролей пользователя. Передайте все роли, которые должны быть у пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Роли обновлены"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    })
    @PutMapping("/{id}/roles")
    public ResponseEntity<UserResponse> updateUserRoles(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id,
            @Parameter(description = "Новый набор ролей", required = true) @Valid @RequestBody UpdateUserRolesRequest request) {
        log.info("Обновление ролей пользователя ID={}", id);
        UserResponse updatedUser = adminService.updateUserRoles(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Удалить пользователя", description = "Удаление пользователя из системы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь удален"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.info("Удаление пользователя ID={}", id);
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Заблокировать пользователя", description = "Блокировка доступа пользователя к системе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь заблокирован"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    })
    @PostMapping("/{id}/disable")
    public ResponseEntity<UserResponse> disableUser(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.info("Блокировка пользователя ID={}", id);
        UserResponse updatedUser = adminService.disableUser(id);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Разблокировать пользователя", description = "Разблокировка доступа пользователя к системе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь разблокирован"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    })
    @PostMapping("/{id}/enable")
    public ResponseEntity<UserResponse> enableUser(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.info("Разблокировка пользователя ID={}", id);
        UserResponse updatedUser = adminService.enableUser(id);
        return ResponseEntity.ok(updatedUser);
    }
}
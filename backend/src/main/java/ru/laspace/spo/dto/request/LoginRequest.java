package ru.laspace.spo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на аутентификацию")
public class LoginRequest {
    @NotBlank(message = "Имя пользователя обязательно")
    @Schema(description = "Имя пользователя", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Schema(description = "Пароль пользователя", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String password;
}

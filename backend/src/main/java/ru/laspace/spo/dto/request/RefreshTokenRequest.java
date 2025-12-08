package ru.laspace.spo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на обновление токена")
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token обязателен")
    @Schema(description = "RefreshToken", example = "exgff-----.eysafh------.signature", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}

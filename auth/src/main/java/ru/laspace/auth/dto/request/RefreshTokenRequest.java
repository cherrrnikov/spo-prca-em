package ru.laspace.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "Запрос на обновление токена")
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token обязателен")
    @Schema(description = "RefreshToken", example = "exgff-----.eysafh------.signature", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}

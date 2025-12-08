package ru.laspace.spo.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ с JWT")
public class JwtResponse {
    @Schema(description = "AccessToken для доступа к API")
    private String accessToken;

    @Schema(description = "RefreshToken для получения новых accessToken")
    private String refreshToken;

    @Builder.Default
    @Schema(description = "Тип токена")
    private String tokenType = "Bearer";

    @Schema(description = "ID пользователя")
    private Long userId;
    @Schema(description = "Имя пользователя")
    private String username;
    @Schema(description = "Имя")
    private String firstName;
    @Schema(description = "Фамилия")
    private String lastName;
    @Schema(description = "Роли пользователя")
    private Set<String> roles;

    @Schema(description = "Время последнего входа")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;
}

package ru.laspace.auth.dto.response;

import java.util.Set;

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
    @Schema(description = "Имя пользователя")
    private String username;
    @Schema(description = "Имя")
    private String firstName;
    @Schema(description = "Фамилия")
    private String lastName;
    @Schema(description = "Роли пользователя")
    private Set<String> roles;

}

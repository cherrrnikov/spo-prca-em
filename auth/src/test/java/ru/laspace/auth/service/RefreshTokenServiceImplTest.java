package ru.laspace.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.laspace.auth.entity.RefreshToken;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.exception.TokenRefreshException;
import ru.laspace.auth.repository.RefreshTokenRepository;
import ru.laspace.auth.service.impl.RefreshTokenServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для RefreshTokenServiceImpl")
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User testUser;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setToken("testRefreshToken");
        refreshToken.setUser(testUser);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("createRefreshToken - успешное создание refresh token")
    void createRefreshToken_WhenValidParameters_ReturnsRefreshToken() {
        String tokenValue = "newRefreshToken";

        RefreshToken result = refreshTokenService.createRefreshToken(testUser, tokenValue);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(tokenValue);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getExpiryDate()).isAfter(LocalDateTime.now().plusDays(6)); // Примерно +7 дней
        assertThat(result.getExpiryDate()).isBefore(LocalDateTime.now().plusDays(8));
    }

    @Test
    @DisplayName("createRefreshToken - разные токены для разных пользователей")
    void createRefreshToken_WithDifferentTokens_CreatesDifferentTokens() {
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");

        String token1 = "token1";
        String token2 = "token2";

        RefreshToken result1 = refreshTokenService.createRefreshToken(testUser, token1);
        RefreshToken result2 = refreshTokenService.createRefreshToken(user2, token2);

        assertThat(result1.getToken()).isEqualTo(token1);
        assertThat(result1.getUser()).isEqualTo(testUser);

        assertThat(result2.getToken()).isEqualTo(token2);
        assertThat(result2.getUser()).isEqualTo(user2);

        assertThat(result1.getToken()).isNotEqualTo(result2.getToken());
        assertThat(result1.getUser()).isNotEqualTo(result2.getUser());
    }

    @Test
    @DisplayName("verifyRefreshToken - успешная проверка валидного токена")
    void verifyRefreshToken_WhenValidToken_ReturnsToken() {
        when(refreshTokenRepository.findByToken("validToken")).thenReturn(Optional.of(refreshToken));

        RefreshToken result = refreshTokenService.verifyRefreshToken("validToken");

        assertThat(result).isEqualTo(refreshToken);
        verify(refreshTokenRepository).findByToken("validToken");
    }

    @Test
    @DisplayName("verifyRefreshToken - токен не найден")
    void verifyRefreshToken_WhenTokenNotFound_ThrowsException() {
        when(refreshTokenRepository.findByToken("nonexistentToken")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("nonexistentToken"))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessage("RefreshToken не найден");
    }

    @Test
    @DisplayName("verifyRefreshToken - истекший токен")
    void verifyRefreshToken_WhenTokenExpired_ThrowsException() {
        refreshToken.setExpiryDate(LocalDateTime.now().minusDays(1)); // Истек вчера
        when(refreshTokenRepository.findByToken("expiredToken")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("expiredToken"))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessage("RefreshToken истёк");
    }

    @Test
    @DisplayName("verifyRefreshToken - токен с точным временем истечения")
    void verifyRefreshToken_WhenTokenExactlyExpired_ThrowsException() {
        refreshToken.setExpiryDate(LocalDateTime.now().minusSeconds(1)); // Истек 1 секунду назад
        when(refreshTokenRepository.findByToken("justExpiredToken")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("justExpiredToken"))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessage("RefreshToken истёк");
    }

    @Test
    @DisplayName("verifyRefreshToken - отозванный токен")
    void verifyRefreshToken_WhenTokenRevoked_ThrowsException() {
        refreshToken.setRevoked(true);
        when(refreshTokenRepository.findByToken("revokedToken")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("revokedToken"))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessage("RefreshToken аннулирован");
    }

    @Test
    @DisplayName("verifyRefreshToken - токен с истечением в будущем")
    void verifyRefreshToken_WhenTokenNotExpired_ReturnsToken() {
        refreshToken.setExpiryDate(LocalDateTime.now().plusHours(1)); // Истечет через час
        when(refreshTokenRepository.findByToken("futureToken")).thenReturn(Optional.of(refreshToken));

        RefreshToken result = refreshTokenService.verifyRefreshToken("futureToken");

        assertThat(result).isEqualTo(refreshToken);
        assertThat(result.getExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("revokeRefreshToken - успешный отзыв токена")
    void revokeRefreshToken_WhenValidTokenAndUser_RevokesToken() {
        when(refreshTokenRepository.findByToken("validToken")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken);

        refreshTokenService.revokeRefreshToken("validToken", 1L);

        verify(refreshTokenRepository).save(refreshToken);
        assertThat(refreshToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("revokeRefreshToken - токен не найден")
    void revokeRefreshToken_WhenTokenNotFound_DoesNothing() {
        when(refreshTokenRepository.findByToken("nonexistentToken")).thenReturn(Optional.empty());

        refreshTokenService.revokeRefreshToken("nonexistentToken", 1L);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("revokeRefreshToken - попытка отозвать чужой токен")
    void revokeRefreshToken_WhenTokenBelongsToAnotherUser_ThrowsException() {
        when(refreshTokenRepository.findByToken("otherUserToken")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.revokeRefreshToken("otherUserToken", 2L))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessage("Refresh token не принадлежит пользователю");

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        assertThat(refreshToken.isRevoked()).isFalse(); // Не отозван
    }

    @Test
    @DisplayName("revokeRefreshToken - отзыв уже отозванного токена")
    void revokeRefreshToken_WhenTokenAlreadyRevoked_StillSaves() {
        refreshToken.setRevoked(true);
        when(refreshTokenRepository.findByToken("alreadyRevoked")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken);

        refreshTokenService.revokeRefreshToken("alreadyRevoked", 1L);

        verify(refreshTokenRepository).save(refreshToken);
        assertThat(refreshToken.isRevoked()).isTrue(); // Остается true
    }

    @Test
    @DisplayName("revokeRefreshToken - исключение при сохранении")
    void revokeRefreshToken_WhenSaveFails_ThrowsException() {
        when(refreshTokenRepository.findByToken("validToken")).thenReturn(Optional.of(refreshToken));
        doThrow(new RuntimeException("Database error")).when(refreshTokenRepository).save(refreshToken);

        assertThatThrownBy(() -> refreshTokenService.revokeRefreshToken("validToken", 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }

    @Test
    @DisplayName("createRefreshToken - проверка срока действия токена")
    void createRefreshToken_ShouldHaveCorrectExpiryDate() {
        String tokenValue = "testToken";
        LocalDateTime beforeCreation = LocalDateTime.now();

        RefreshToken result = refreshTokenService.createRefreshToken(testUser, tokenValue);
        LocalDateTime afterCreation = LocalDateTime.now();

        assertThat(result.getExpiryDate()).isAfter(beforeCreation.plusDays(6).plusHours(23).plusMinutes(59));
        assertThat(result.getExpiryDate()).isBefore(afterCreation.plusDays(7).plusMinutes(1));

        long hoursBetween = java.time.Duration.between(LocalDateTime.now(), result.getExpiryDate()).toHours();
        assertThat(hoursBetween).isBetween(167L, 169L);
    }

    @Test
    @DisplayName("createRefreshToken - все поля установлены корректно")
    void createRefreshToken_AllFieldsSetCorrectly() {
        String tokenValue = "completeToken";

        RefreshToken result = refreshTokenService.createRefreshToken(testUser, tokenValue);

        assertThat(result.getId()).isNull();
        assertThat(result.getToken()).isEqualTo(tokenValue);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getExpiryDate()).isNotNull();
        assertThat(result.getExpiryDate()).isAfter(result.getCreatedAt());
    }
}
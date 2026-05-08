package ru.laspace.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.laspace.auth.config.JwtProperties;
import ru.laspace.auth.entity.RefreshToken;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.exception.AuthException;
import ru.laspace.auth.repository.RefreshTokenRepository;
import ru.laspace.auth.security.JwtService;
import ru.laspace.auth.service.impl.RefreshTokenServiceImpl;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-32-chars!!");
        props.setIssuer("test-issuer");
        props.setAccessTokenExpiration(900_000L);
        props.setRefreshTokenExpiration(604_800_000L);
        jwtService = new JwtService(props);
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, jwtService);
    }

    @Test
    void createRefreshToken_savesAndReturnsToken() {
        User user = buildUser(1L, "igor");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String token = refreshTokenService.createRefreshToken(user, "127.0.0.1", "Mozilla");

        assertThat(token).isNotBlank();
        verify(refreshTokenRepository).revokeAllUserTokens(eq(1L), any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_revokesAllPreviousTokens() {
        User user = buildUser(1L, "igor");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.createRefreshToken(user, "127.0.0.1", "Mozilla");

        verify(refreshTokenRepository).revokeAllUserTokens(eq(1L), any());
    }

    @Test
    void verifyRefreshToken_validToken_returnsToken() {
        String tokenValue = jwtService.generateRefreshToken("igor", 1L);
        RefreshToken refreshToken = buildRefreshToken(tokenValue, false,
                LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByToken(tokenValue))
                .thenReturn(Optional.of(refreshToken));

        RefreshToken result = refreshTokenService.verifyRefreshToken(tokenValue);

        assertThat(result).isEqualTo(refreshToken);
    }

    @Test
    void verifyRefreshToken_revokedToken_throwsAuthException() {
        String tokenValue = jwtService.generateRefreshToken("igor", 1L);
        RefreshToken refreshToken = buildRefreshToken(tokenValue, true,
                LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByToken(tokenValue))
                .thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(tokenValue))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("revoked or expired");
    }

    @Test
    void verifyRefreshToken_expiredToken_throwsAuthException() {
        String tokenValue = jwtService.generateRefreshToken("igor", 1L);
        RefreshToken refreshToken = buildRefreshToken(tokenValue, false,
                LocalDateTime.now().minusDays(1)); // истёк вчера
        when(refreshTokenRepository.findByToken(tokenValue))
                .thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(tokenValue)).isInstanceOf(AuthException.class)
                .hasMessageContaining("revoked or expired");
    }

    @Test
    void verifyRefreshToken_notFoundInDb_throwsAuthException() {
        String tokenValue = jwtService.generateRefreshToken("igor", 1L);
        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(tokenValue))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void verifyRefreshToken_invalidString_throwsAuthException() {
        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("garbage.token.value"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void revokeRefreshToken_revokesExistingToken() {
        String tokenValue = jwtService.generateRefreshToken("igor", 1L);
        RefreshToken refreshToken = buildRefreshToken(tokenValue, false,
                LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByToken(tokenValue))
                .thenReturn(Optional.of(refreshToken));

        refreshTokenService.revokeRefreshToken(tokenValue);

        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(refreshToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void revokeRefreshToken_doesNothingForUnknownToken() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        refreshTokenService.revokeRefreshToken("unknown");

        verify(refreshTokenRepository, never()).save(any());
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        return user;
    }

    private RefreshToken buildRefreshToken(String token, boolean revoked,
            LocalDateTime expiryDate) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(token);
        rt.setRevoked(revoked);
        rt.setExpiryDate(expiryDate);
        rt.setUser(buildUser(1L, "igor"));
        return rt;
    }
}
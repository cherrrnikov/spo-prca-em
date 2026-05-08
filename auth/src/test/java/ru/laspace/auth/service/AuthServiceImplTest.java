package ru.laspace.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import ru.laspace.auth.config.JwtProperties;
import ru.laspace.auth.config.SecurityProperties;
import ru.laspace.auth.dto.request.LoginRequest;
import ru.laspace.auth.dto.request.RefreshTokenRequest;
import ru.laspace.auth.dto.response.JwtResponse;
import ru.laspace.auth.entity.RefreshToken;
import ru.laspace.auth.entity.Role;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.exception.AuthException;
import ru.laspace.auth.repository.RoleRepository;
import ru.laspace.auth.repository.UserRepository;
import ru.laspace.auth.security.CustomUserDetails;
import ru.laspace.auth.security.JwtService;
import ru.laspace.auth.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserCacheService userCacheService;
    @Mock
    private HttpServletRequest httpRequest;

    private AuthServiceImpl authService;
    private JwtService jwtService;
    private SecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-must-be-at-least-32-chars!!");
        jwtProperties.setIssuer("test-issuer");
        jwtProperties.setAccessTokenExpiration(900_000L);
        jwtProperties.setRefreshTokenExpiration(604_800_000L);
        jwtService = new JwtService(jwtProperties);

        securityProperties = new SecurityProperties();
        securityProperties.setMaxFailedAttempts(5);
        securityProperties.setAccountLockDurationMinutes(15);
        securityProperties.setLoginDelayMillis(0);

        authService = new AuthServiceImpl(
                authenticationManager, jwtService, userRepository, roleRepository,
                refreshTokenService, loginAttemptService, passwordEncoder,
                securityProperties, userCacheService);
    }

    // --- login ---

    @Test
    void login_success_returnsJwtResponse() {
        User user = buildUser(1L, "igor", false, true);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        var authToken = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());

        when(loginAttemptService.isAccountLocked("igor")).thenReturn(false);
        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(userRepository.save(any())).thenReturn(user);
        when(refreshTokenService.createRefreshToken(any(), any(), any()))
                .thenReturn("refresh-token-value");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla");

        JwtResponse response = authService.login(
                new LoginRequest("igor", "password"), httpRequest);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-value");
        assertThat(response.getUsername()).isEqualTo("igor");
    }

    @Test
    void login_lockedAccount_throwsAuthException() {
        when(loginAttemptService.isAccountLocked("igor")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("igor", "password"), httpRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("locked");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_badCredentials_throwsAuthExceptionAndRecordsFailure() {
        when(loginAttemptService.isAccountLocked("igor")).thenReturn(false);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("igor", "wrongpassword"), httpRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid username or password");

        verify(loginAttemptService).loginFailed("igor");
    }

    // --- refresh ---

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        User user = buildUser(1L, "igor", false, true);
        RefreshToken refreshToken = buildRefreshToken("refresh-token-value", user);

        when(refreshTokenService.verifyRefreshToken("refresh-token-value"))
                .thenReturn(refreshToken);
        when(userRepository.save(any())).thenReturn(user);

        JwtResponse response = authService.refresh(
                new RefreshTokenRequest("refresh-token-value"));

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo("igor");
    }

    @Test
    void refresh_disabledUser_throwsAuthException() {
        User user = buildUser(1L, "igor", false, false); // enabled=false
        RefreshToken refreshToken = buildRefreshToken("refresh-token-value", user);

        when(refreshTokenService.verifyRefreshToken("refresh-token-value"))
                .thenReturn(refreshToken);

        assertThatThrownBy(() -> authService.refresh(
                new RefreshTokenRequest("refresh-token-value")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void refresh_lockedUser_throwsAuthException() {
        User user = buildUser(1L, "igor", true, true); // accountLocked=true
        user.setLockTime(LocalDateTime.now()); // блокировка только что
        RefreshToken refreshToken = buildRefreshToken("refresh-token-value", user);

        when(refreshTokenService.verifyRefreshToken("refresh-token-value"))
                .thenReturn(refreshToken);

        assertThatThrownBy(() -> authService.refresh(
                new RefreshTokenRequest("refresh-token-value")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("locked");
    }

    // --- logout ---

    @Test
    void logout_validToken_revokesToken() {
        User user = buildUser(1L, "igor", false, true);
        RefreshToken refreshToken = buildRefreshToken("refresh-token-value", user);

        when(refreshTokenService.verifyRefreshToken("refresh-token-value"))
                .thenReturn(refreshToken);
        when(userRepository.save(any())).thenReturn(user);

        authService.logout("refresh-token-value");

        verify(refreshTokenService).revokeRefreshToken("refresh-token-value");
        verify(userRepository).save(user);
    }

    @Test
    void logout_invalidToken_doesNotThrow() {
        when(refreshTokenService.verifyRefreshToken("bad-token"))
                .thenThrow(new AuthException("Invalid token"));

        // logout не должен пробрасывать исключение — он глотает ошибки
        authService.logout("bad-token");

        verify(userRepository, never()).save(any());
    }

    // --- вспомогательные методы ---

    private User buildUser(Long id, String username,
            boolean accountLocked, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setFirstName("Игорь");
        user.setLastName("Тестов");
        user.setEnabled(enabled);
        user.setAccountLocked(accountLocked);
        user.setFailedAttempts(0);
        user.setRoles(new HashSet<>(Set.of(buildRole("OPERATOR"))));
        return user;
    }

    private Role buildRole(String name) {
        Role role = new Role();
        role.setId(1L);
        role.setName(name);
        return role;
    }

    private RefreshToken buildRefreshToken(String tokenValue, User user) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(tokenValue);
        rt.setUser(user);
        rt.setRevoked(false);
        rt.setExpiryDate(LocalDateTime.now().plusDays(7));
        return rt;
    }
}

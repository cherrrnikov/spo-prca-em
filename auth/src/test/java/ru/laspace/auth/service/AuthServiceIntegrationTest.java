package ru.laspace.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import ru.laspace.auth.TestcontainersConfiguration;
import ru.laspace.auth.dto.request.LoginRequest;
import ru.laspace.auth.dto.request.RefreshTokenRequest;
import ru.laspace.auth.dto.response.JwtResponse;
import ru.laspace.auth.entity.Role;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.exception.AuthException;
import ru.laspace.auth.repository.RefreshTokenRepository;
import ru.laspace.auth.repository.RoleRepository;
import ru.laspace.auth.repository.UserRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private MockHttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("127.0.0.1");
        httpRequest.addHeader("User-Agent", "TestAgent");

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setName("OPERATOR");
        role.setDescription("Оператор");
        Role savedRole = roleRepository.save(role);

        User user = new User();
        user.setUsername("igor");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFirstName("Игорь");
        user.setLastName("Тестов");
        user.setEnabled(true);
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setRoles(new HashSet<>(Set.of(savedRole)));
        userRepository.save(user);
    }

    // --- login ---

    @Test
    void login_validCredentials_returnsTokens() {
        JwtResponse response = authService.login(
                new LoginRequest("igor", "password123"), httpRequest);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo("igor");
    }

    @Test
    void login_wrongPassword_throwsAuthException() {
        assertThatThrownBy(() -> authService.login(
                new LoginRequest("igor", "wrongpassword"), httpRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_unknownUser_throwsAuthException() {
        assertThatThrownBy(() -> authService.login(
                new LoginRequest("unknown", "password123"), httpRequest))
                .isInstanceOf(AuthException.class);
    }

    // --- refresh ---

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        JwtResponse loginResponse = authService.login(
                new LoginRequest("igor", "password123"), httpRequest);

        JwtResponse refreshResponse = authService.refresh(
                new RefreshTokenRequest(loginResponse.getRefreshToken()));

        assertThat(refreshResponse.getAccessToken()).isNotBlank();
        assertThat(refreshResponse.getUsername()).isEqualTo("igor");
    }

    @Test
    void refresh_invalidToken_throwsAuthException() {
        assertThatThrownBy(() -> authService.refresh(
                new RefreshTokenRequest("invalid.token.value")))
                .isInstanceOf(AuthException.class);
    }

    // --- logout ---

    @Test
    void logout_validToken_revokesRefreshToken() {
        JwtResponse loginResponse = authService.login(
                new LoginRequest("igor", "password123"), httpRequest);

        authService.logout(loginResponse.getRefreshToken());

        assertThatThrownBy(() -> authService.refresh(
                new RefreshTokenRequest(loginResponse.getRefreshToken())))
                .isInstanceOf(AuthException.class);
    }

    // --- полный цикл ---

    @Test
    void fullCycle_loginRefreshLogout() {
        JwtResponse loginResponse = authService.login(
                new LoginRequest("igor", "password123"), httpRequest);
        assertThat(loginResponse.getAccessToken()).isNotBlank();

        JwtResponse refreshResponse = authService.refresh(
                new RefreshTokenRequest(loginResponse.getRefreshToken()));
        assertThat(refreshResponse.getAccessToken()).isNotBlank();

        authService.logout(loginResponse.getRefreshToken());

        assertThatThrownBy(() -> authService.refresh(
                new RefreshTokenRequest(loginResponse.getRefreshToken())))
                .isInstanceOf(AuthException.class);
    }
}
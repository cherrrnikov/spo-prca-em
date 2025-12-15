package ru.laspace.spo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.laspace.spo.dto.request.LoginRequest;
import ru.laspace.spo.entity.Role;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.repository.RoleRepository;
import ru.laspace.spo.repository.UserRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Интеграционные тесты для AuthController")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("security.enable-brute-force-protection", () -> "false");
        registry.add("jwt.secret", () -> "test-secret-key-for-jwt-token-generation-in-tests-only");
        registry.add("jwt.access-token-expiration", () -> "900000");
        registry.add("jwt.refresh-token-expiration", () -> "604800000");
        registry.add("jwt.issuer", () -> "test-app");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role userRole;
    private User testUser;
    private String rawPassword = "password123";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        userRole = new Role();
        userRole.setName("USER");
        userRole.setDescription("Обычный пользователь");
        userRole = roleRepository.save(userRole);

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash(passwordEncoder.encode(rawPassword)); // ИСПРАВЛЕНО
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setRoles(new HashSet<>(Set.of(userRole)));
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("POST /api/auth/login - успешный вход")
    void login_WithValidCredentials_ReturnsJwtTokens() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new LoginRequest("testuser", rawPassword) // Используем тот же пароль
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("POST /api/auth/login - неверный пароль")
    void login_WithInvalidPassword_ReturnsUnauthorized() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new LoginRequest("testuser", "wrongpassword"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - отключенный аккаунт")
    void login_WithDisabledAccount_ReturnsUnauthorized() throws Exception {
        testUser.setEnabled(false);
        userRepository.save(testUser); // Это update, а не insert

        String requestBody = objectMapper.writeValueAsString(
                new LoginRequest("testuser", rawPassword));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - заблокированный аккаунт (без brute force защиты)")
    void login_WithLockedAccount_ReturnsUnauthorized() throws Exception {
        testUser.setEnabled(false);
        testUser.setAccountLocked(true);
        userRepository.save(testUser); // Это update, а не insert

        String requestBody = objectMapper.writeValueAsString(
                new LoginRequest("testuser", rawPassword));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - валидация запроса (пустой username)")
    void login_WithEmptyUsername_ReturnsBadRequest() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new LoginRequest("", rawPassword));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/auth/validate - без токена")
    void validateToken_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized());
    }
}
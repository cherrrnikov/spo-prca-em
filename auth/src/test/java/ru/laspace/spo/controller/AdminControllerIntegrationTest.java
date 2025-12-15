package ru.laspace.spo.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.laspace.spo.entity.Role;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.repository.RoleRepository;
import ru.laspace.spo.repository.UserRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Интеграционные тесты для AdminController")
class AdminControllerIntegrationTest {

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
        registry.add("spring.cache.type", () -> "none");
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
    private Role adminRole;
    private User regularUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        userRole = new Role();
        userRole.setName("USER");
        userRole.setDescription("Обычный пользователь");
        userRole = roleRepository.save(userRole);

        adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("Администратор");
        adminRole = roleRepository.save(adminRole);

        regularUser = new User();
        regularUser.setUsername("regular");
        regularUser.setPasswordHash(passwordEncoder.encode("password123")); // ИСПРАВЛЕНО
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        regularUser.setEnabled(true);

        java.util.Set<Role> userRoles = new java.util.HashSet<>();
        userRoles.add(userRole);
        regularUser.setRoles(userRoles);
        regularUser = userRepository.save(regularUser);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/users - получение всех пользователей (админ)")
    void getAllUsers_AsAdmin_ReturnsUsersList() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("regular"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/users - создание пользователя (админ)")
    void createUser_AsAdmin_ReturnsCreated() throws Exception {
        String requestBody = """
                {
                    "username": "newuser",
                    "password": "newpassword123",
                    "firstName": "New",
                    "lastName": "User",
                    "enabled": true,
                    "roles": ["USER"]
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/users - пустой список когда нет пользователей")
    void getAllUsers_WhenNoUsers_ReturnsEmptyList() throws Exception {
        userRepository.deleteAll();

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/users/{id}/disable - блокировка пользователя")
    void disableUser_AsAdmin_ReturnsDisabledUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/{id}/disable", regularUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("regular"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/users/{id}/unlock - разблокировка аккаунта")
    void unlockUserAccount_AsAdmin_UnlocksAccount() throws Exception {
        User lockedUser = new User();
        lockedUser.setUsername("locked");
        lockedUser.setPasswordHash(passwordEncoder.encode("password123"));
        lockedUser.setFirstName("Locked");
        lockedUser.setLastName("User");
        lockedUser.setEnabled(true);
        lockedUser.setAccountLocked(true);
        lockedUser.setFailedAttempts(5);

        java.util.Set<Role> roles = new java.util.HashSet<>();
        roles.add(userRole);
        lockedUser.setRoles(roles);
        lockedUser = userRepository.save(lockedUser);

        mockMvc.perform(post("/api/admin/users/{id}/unlock", lockedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("locked"))
                .andExpect(jsonPath("$.accountLocked").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/admin/users - без прав админа")
    void createUser_AsRegularUser_ReturnsForbidden() throws Exception {
        String requestBody = """
                {
                    "username": "newuser",
                    "password": "password123",
                    "firstName": "New",
                    "lastName": "User"
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Все endpoints - без аутентификации")
    void allEndpoints_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isUnauthorized());
    }
}
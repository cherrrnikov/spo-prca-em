package ru.laspace.spo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.laspace.spo.entity.Role;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.repository.RoleRepository;
import ru.laspace.spo.repository.UserRepository;
import ru.laspace.spo.security.JwtProvider;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
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

    @Autowired
    private JwtProvider jwtProvider;

    private User adminUser;
    private User regularUser;
    private Role userRole;
    private Role adminRole;
    private String adminToken;
    private String userToken;

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

        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setEnabled(true);

        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        adminRoles.add(userRole);
        adminUser.setRoles(adminRoles);

        adminUser = userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setUsername("regular");
        regularUser.setPasswordHash(passwordEncoder.encode("password123"));
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        regularUser.setEnabled(true);

        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        regularUser.setRoles(userRoles);

        regularUser = userRepository.save(regularUser);

        Set<String> adminRoleNames = Set.of("ADMIN", "USER");
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                adminRoleNames.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList()));

        adminToken = jwtProvider.generateAccessToken(adminAuth, adminUser.getId(), adminRoleNames);

        Set<String> userRoleNames = Set.of("USER");
        Authentication userAuth = new UsernamePasswordAuthenticationToken(
                "regular",
                null,
                userRoleNames.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList()));

        userToken = jwtProvider.generateAccessToken(userAuth, regularUser.getId(), userRoleNames);
    }

    @Test
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
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @DisplayName("POST /api/admin/users - создание пользователя с несколькими ролями")
    void createUser_WithMultipleRoles_ReturnsCreated() throws Exception {
        String requestBody = """
                {
                    "username": "moderator",
                    "password": "modpass123",
                    "firstName": "Mod",
                    "lastName": "Erator",
                    "enabled": true,
                    "roles": ["USER", "ADMIN"]
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasSize(2)))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER", "ADMIN")));
    }

    @Test
    @DisplayName("POST /api/admin/users - дублирование username")
    void createUser_WithExistingUsername_ReturnsConflict() throws Exception {
        String requestBody = """
                {
                    "username": "regular",
                    "password": "newpass123",
                    "firstName": "Duplicate",
                    "lastName": "User"
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
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
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/users - получение всех пользователей (админ)")
    void getAllUsers_AsAdmin_ReturnsUsersList() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2))) // admin и regular
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("admin", "regular")));
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} - получение пользователя по ID")
    void getUserById_AsAdmin_ReturnsUser() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", regularUser.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(regularUser.getId().intValue()))
                .andExpect(jsonPath("$.username").value("regular"))
                .andExpect(jsonPath("$.firstName").value("Regular"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} - пользователь не найден")
    void getUserById_WhenUserNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", 9999)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - обновление ролей пользователя")
    void updateUserRoles_AsAdmin_UpdatesRoles() throws Exception {
        String requestBody = """
                {
                    "roles": ["ADMIN"]
                }
                """;

        mockMvc.perform(put("/api/admin/users/{id}/roles", regularUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(regularUser.getId().intValue()))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasSize(1)))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - очистка всех ролей")
    void updateUserRoles_WithEmptyRoles_ClearsAllRoles() throws Exception {
        String requestBody = """
                {
                    "roles": []
                }
                """;

        mockMvc.perform(put("/api/admin/users/{id}/roles", regularUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasSize(0)));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - несуществующая роль")
    void updateUserRoles_WithNonExistentRole_ReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "roles": ["NONEXISTENT"]
                }
                """;

        mockMvc.perform(put("/api/admin/users/{id}/roles", regularUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id} - удаление пользователя")
    void deleteUser_AsAdmin_ReturnsNoContent() throws Exception {
        User tempUser = new User();
        tempUser.setUsername("todelete");
        tempUser.setPasswordHash(passwordEncoder.encode("password123"));
        tempUser.setFirstName("To");
        tempUser.setLastName("Delete");
        tempUser.setEnabled(true);
        tempUser.setRoles(Set.of(userRole));
        tempUser = userRepository.save(tempUser);

        mockMvc.perform(delete("/api/admin/users/{id}", tempUser.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/users/{id}", tempUser.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/disable - блокировка пользователя")
    void disableUser_AsAdmin_ReturnsDisabledUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/{id}/disable", regularUser.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(regularUser.getId().intValue()))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/enable - разблокировка пользователя")
    void enableUser_AsAdmin_ReturnsEnabledUser() throws Exception {
        regularUser.setEnabled(false);
        userRepository.save(regularUser);

        mockMvc.perform(post("/api/admin/users/{id}/enable", regularUser.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(regularUser.getId().intValue()))
                .andExpect(jsonPath("$.enabled").value(true));
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

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Валидация запроса - пустые поля")
    void createUser_WithInvalidRequest_ReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "username": "",
                    "password": "",
                    "firstName": "",
                    "lastName": ""
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/admin/users - проверка хэширования пароля")
    void createUser_VerifiesPasswordHashing() throws Exception {
        String rawPassword = "plainpassword123";
        String requestBody = String.format("""
                {
                    "username": "hasheduser",
                    "password": "%s",
                    "firstName": "Hashed",
                    "lastName": "User",
                    "roles": ["USER"]
                }
                """, rawPassword);

        String response = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long userId = objectMapper.readTree(response).get("id").asLong();

        User createdUser = userRepository.findById(userId).orElseThrow();
        assertThat(passwordEncoder.matches(rawPassword, createdUser.getPasswordHash())).isTrue();
        assertThat(createdUser.getPasswordHash()).isNotEqualTo(rawPassword);
    }
}
package ru.laspace.spo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ru.laspace.spo.dto.request.CreateUserRequest;
import ru.laspace.spo.dto.request.UpdateUserRolesRequest;
import ru.laspace.spo.dto.response.UserResponse;
import ru.laspace.spo.entity.Role;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.exception.NotFoundException;
import ru.laspace.spo.repository.RoleRepository;
import ru.laspace.spo.repository.UserRepository;
import ru.laspace.spo.service.impl.AdminServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для AdminServiceImpl")
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;
    private Role userRole;
    private Role adminRole;
    private Set<Role> roles;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");

        roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setRoles(roles);
    }

    @Test
    @DisplayName("createUser - успешное создание пользователя без ролей")
    void createUser_WhenValidRequestWithoutRoles_CreatesUser() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .enabled(true)
                .roles(null)
                .build();

        User newUser = new User();
        newUser.setId(2L);
        newUser.setUsername("newuser");
        newUser.setPasswordHash("encodedPassword");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setEnabled(true);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        UserResponse response = adminService.createUser(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getFirstName()).isEqualTo("New");
        assertThat(response.getLastName()).isEqualTo("User");
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getRoles()).isEmpty();

        verify(userRepository).existsByUsername("newuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser - успешное создание пользователя с ролями")
    void createUser_WhenValidRequestWithRoles_CreatesUserWithRoles() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .enabled(true)
                .roles(Set.of("USER", "ADMIN"))
                .build();

        User newUser = new User();
        newUser.setId(2L);
        newUser.setUsername("newuser");
        newUser.setPasswordHash("encodedPassword");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setEnabled(true);
        newUser.setRoles(roles);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        UserResponse response = adminService.createUser(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");

        verify(roleRepository).findByName("USER");
        verify(roleRepository).findByName("ADMIN");
    }

    @Test
    @DisplayName("createUser - пользователь уже существует")
    void createUser_WhenUserAlreadyExists_ThrowsException() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("existinguser")
                .password("password123")
                .firstName("Existing")
                .lastName("User")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> adminService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь 'existinguser' уже существует");
    }

    @Test
    @DisplayName("createUser - указана несуществующая роль")
    void createUser_WhenRoleNotFound_ThrowsException() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .roles(Set.of("NONEXISTENT"))
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Роль 'NONEXISTENT' не найдена");
    }

    @Test
    @DisplayName("getAllUsers - получение списка пользователей")
    void getAllUsers_WhenCalled_ReturnsAllUsers() {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");

        List<User> users = Arrays.asList(user1, user2);

        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<UserResponse> result = adminService.getAllUsers();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("user1");
        assertThat(result.get(1).getUsername()).isEqualTo("user2");

        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getUserById - успешное получение пользователя")
    void getUserById_WhenUserExists_ReturnsUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        UserResponse response = adminService.getUserById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("getUserById - пользователь не найден")
    void getUserById_WhenUserNotFound_ThrowsNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.getUserById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с ID=999 не найден");
    }

    @Test
    @DisplayName("getUserByUsername - успешное получение пользователя")
    void getUserByUsername_WhenUserExists_ReturnsUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserResponse response = adminService.getUserByUsername("testuser");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("updateUserRoles - успешное обновление ролей")
    void updateUserRoles_WhenValidRequest_UpdatesRoles() {
        // Arrange
        UpdateUserRolesRequest request = new UpdateUserRolesRequest();
        request.setRoles(Set.of("USER")); // Только USER роль

        // Очищаем текущие роли у пользователя
        testUser.setRoles(new HashSet<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse response = adminService.updateUserRoles(1L, request);

        // Assert
        assertThat(response).isNotNull();
        // Проверяем что вызвалось только для USER
        verify(roleRepository).findByName("USER");
        verify(roleRepository, never()).findByName("MODERATOR"); // MODERATOR не должен вызываться
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUserRoles - очистка всех ролей")
    void updateUserRoles_WhenEmptyRoles_ClearsAllRoles() {
        // Arrange
        UpdateUserRolesRequest request = new UpdateUserRolesRequest();
        request.setRoles(Collections.emptySet());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse response = adminService.updateUserRoles(1L, request);

        // Assert
        assertThat(response).isNotNull();
        // Проверяем, что save был вызван

        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    @DisplayName("updateUserRoles - пользователь не найден")
    void updateUserRoles_WhenUserNotFound_ThrowsNotFoundException() {
        // Arrange
        UpdateUserRolesRequest request = new UpdateUserRolesRequest();
        request.setRoles(Set.of("USER"));

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.updateUserRoles(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с ID=999 не найден");
    }

    @Test
    @DisplayName("deleteUser - успешное удаление пользователя")
    void deleteUser_WhenUserExists_DeletesUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        // Act
        adminService.deleteUser(1L);

        // Assert
        verify(userRepository).findById(1L);
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteUser - пользователь не найден")
    void deleteUser_WhenUserNotFound_ThrowsNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.deleteUser(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с ID=999 не найден");

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("disableUser - успешная блокировка пользователя")
    void disableUser_WhenUserExists_DisablesUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse response = adminService.disableUser(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isEnabled()).isFalse();

        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("enableUser - успешная разблокировка пользователя")
    void enableUser_WhenUserExists_EnablesUser() {
        // Arrange
        testUser.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse response = adminService.enableUser(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isEnabled()).isTrue();

        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("enableUser - пользователь не найден")
    void enableUser_WhenUserNotFound_ThrowsNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.enableUser(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с ID=999 не найден");
    }
}
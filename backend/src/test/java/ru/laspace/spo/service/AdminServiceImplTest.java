package ru.laspace.spo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import ru.laspace.spo.mapper.UserMapper;
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

    @Mock
    private UserMapper userMapper;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;
    private Role userRole;
    private Role adminRole;
    private Set<Role> roles;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");
        userRole.setDescription("Обычный пользователь");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");
        adminRole.setDescription("Администратор");

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
        testUser.setAccountLocked(false);
        testUser.setFailedAttempts(0);
        testUser.setLastLoginAt(LocalDateTime.now().minusDays(1));
        testUser.setLastLogoutAt(LocalDateTime.now().minusHours(2));
        testUser.setRoles(roles);

        userResponse = UserResponse.builder()
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .accountLocked(false)
                .failedAttempts(0)
                .lastLoginAt(testUser.getLastLoginAt())
                .lastLogoutAt(testUser.getLastLogoutAt())
                .roles(Set.of("USER", "ADMIN"))
                .build();
    }

    @Test
    @DisplayName("createUser - пользователь с заблокированным аккаунтом")
    void createUser_WithAccountLocked_CreatesUserWithLockedAccount() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("lockeduser")
                .password("password123")
                .firstName("Locked")
                .lastName("User")
                .enabled(true)
                .accountLocked(true)
                .failedAttempts(3)
                .roles(Set.of("USER"))
                .build();

        User lockedUser = new User();
        lockedUser.setId(2L);
        lockedUser.setUsername("lockeduser");
        lockedUser.setPasswordHash("encodedPassword");
        lockedUser.setFirstName("Locked");
        lockedUser.setLastName("User");
        lockedUser.setEnabled(true);
        lockedUser.setAccountLocked(true);
        lockedUser.setFailedAttempts(3);

        UserResponse lockedResponse = UserResponse.builder()
                .username("lockeduser")
                .firstName("Locked")
                .lastName("User")
                .enabled(true)
                .accountLocked(true)
                .failedAttempts(3)
                .roles(Set.of("USER"))
                .build();

        when(userRepository.existsByUsername("lockeduser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(lockedUser);
        when(userMapper.toResponse(lockedUser)).thenReturn(lockedResponse);

        UserResponse response = adminService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.isAccountLocked()).isTrue();
        assertThat(response.getFailedAttempts()).isEqualTo(3);
        verify(userRepository).existsByUsername("lockeduser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("getAllUsers - пустой список пользователей")
    void getAllUsers_WhenNoUsers_ReturnsEmptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<UserResponse> result = adminService.getAllUsers();

        assertThat(result).isEmpty();
        verify(userRepository).findAll();
        verify(userMapper, never()).toResponse(any(User.class));
    }

    @Test
    @DisplayName("getUserByUsername - пользователь не найден")
    void getUserByUsername_WhenUserNotFound_ThrowsNotFoundException() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserByUsername("nonexistent"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь 'nonexistent' не найден");
    }

    @Test
    @DisplayName("updateUserRoles - роли null (очистка всех ролей)")
    void updateUserRoles_WhenRolesNull_ClearsAllRoles() {
        UpdateUserRolesRequest request = new UpdateUserRolesRequest();
        request.setRoles(null); // Явно устанавливаем null

        testUser.setRoles(new HashSet<>(roles));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(userResponse);

        UserResponse response = adminService.updateUserRoles(1L, request);

        assertThat(response).isNotNull();
        verify(userRepository).save(testUser);
        assertThat(testUser.getRoles()).isEmpty();
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    @DisplayName("disableUser - пользователь уже отключен")
    void disableUser_WhenUserAlreadyDisabled_KeepsDisabled() {
        testUser.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse disabledResponse = UserResponse.builder()
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .enabled(false)
                .accountLocked(false)
                .failedAttempts(0)
                .roles(Set.of("USER", "ADMIN"))
                .build();
        when(userMapper.toResponse(testUser)).thenReturn(disabledResponse);

        UserResponse response = adminService.disableUser(1L);

        assertThat(response).isNotNull();
        assertThat(response.isEnabled()).isFalse();
        verify(userRepository).save(testUser);
        assertThat(testUser.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("enableUser - пользователь не найден")
    void enableUser_WhenUserNotFound_ThrowsNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.enableUser(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с ID=999 не найден");
    }

    @Test
    @DisplayName("unlockUserAccount - успешная разблокировка аккаунта")
    void unlockUserAccount_WhenUserExists_UnlocksAccount() {
        testUser.setAccountLocked(true);
        testUser.setFailedAttempts(5);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(10));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(loginAttemptService).unlockAccount("testuser");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(userResponse);

        UserResponse response = adminService.unlockUserAccount(1L);

        assertThat(response).isNotNull();
        verify(loginAttemptService).unlockAccount("testuser");
        verify(userRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("unlockUserAccount - пользователь не найден")
    void unlockUserAccount_WhenUserNotFound_ThrowsNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.unlockUserAccount(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с ID=999 не найден");
    }

    @Test
    @DisplayName("resetUserPassword - успешный сброс пароля")
    void resetUserPassword_WhenUserExists_ResetsPassword() {
        testUser.setFailedAttempts(3);
        testUser.setAccountLocked(true);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(5));

        String newPassword = "NewSecurePassword123!";
        String encodedPassword = "newEncodedPassword";

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse resetResponse = UserResponse.builder()
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .accountLocked(false)
                .failedAttempts(0)
                .roles(Set.of("USER", "ADMIN"))
                .build();
        when(userMapper.toResponse(testUser)).thenReturn(resetResponse);

        UserResponse response = adminService.resetUserPassword(1L, newPassword);

        assertThat(response).isNotNull();
        assertThat(response.isAccountLocked()).isFalse();
        assertThat(response.getFailedAttempts()).isEqualTo(0);

        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);

        assertThat(testUser.getPasswordHash()).isEqualTo(encodedPassword);
        assertThat(testUser.getFailedAttempts()).isEqualTo(0);
        assertThat(testUser.isAccountLocked()).isFalse();
        assertThat(testUser.getLockTime()).isNull();
    }

    @Test
    @DisplayName("resetUserPassword - пользователь не найден")
    void resetUserPassword_WhenUserNotFound_ThrowsNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.resetUserPassword(999L, "newPassword"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с ID=999 не найден");
    }

    @Test
    @DisplayName("resetUserPassword - сброс пароля для пользователя без блокировки")
    void resetUserPassword_WhenUserNotLocked_StillResetsAttempts() {
        testUser.setFailedAttempts(2);
        testUser.setAccountLocked(false);
        testUser.setLockTime(null);

        String newPassword = "NewPassword123";
        String encodedPassword = "encodedNewPassword";

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(userResponse);

        UserResponse response = adminService.resetUserPassword(1L, newPassword);

        assertThat(response).isNotNull();
        verify(userRepository).save(testUser);
        assertThat(testUser.getFailedAttempts()).isEqualTo(0);
        assertThat(testUser.getPasswordHash()).isEqualTo(encodedPassword);
    }

    @Test
    @DisplayName("deleteUser - пользователь не найден")
    void deleteUser_WhenUserNotFound_ThrowsNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteUser(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с ID=999 не найден");

        verify(userRepository, never()).delete(any(User.class));
    }
}
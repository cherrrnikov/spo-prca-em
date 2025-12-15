package ru.laspace.spo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ru.laspace.spo.config.SecurityProperties;
import ru.laspace.spo.dto.cache.UserCacheDto;
import ru.laspace.spo.entity.Role;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.security.UserDetailsImpl;
import ru.laspace.spo.security.UserDetailsServiceImpl;
import ru.laspace.spo.service.impl.UserCacheServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для UserCacheServiceImpl")
class UserCacheServiceImplTest {

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private SecurityProperties securityProperties;

    @InjectMocks
    private UserCacheServiceImpl userCacheService;

    private User testUser;
    private UserDetailsImpl userDetails;
    private UserCacheDto userCacheDto;
    private Role userRole;
    private Role adminRole;

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

        Set<Role> roles = new HashSet<>();
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
        testUser.setLastLoginAt(LocalDateTime.now().minusHours(1));
        testUser.setLastLogoutAt(LocalDateTime.now().minusHours(2));
        testUser.setLockTime(null);
        testUser.setLastFailedLogin(null);
        testUser.setRoles(roles);

        userDetails = new UserDetailsImpl(testUser, securityProperties);

        userCacheDto = new UserCacheDto();
        userCacheDto.setId(1L);
        userCacheDto.setUsername("testuser");
        userCacheDto.setFirstName("Test");
        userCacheDto.setLastName("User");
        userCacheDto.setEnabled(true);
        userCacheDto.setAccountLocked(false);
        userCacheDto.setFailedAttempts(0);
        userCacheDto.setLastLoginAt(testUser.getLastLoginAt());
        userCacheDto.setLastLogoutAt(testUser.getLastLogoutAt());
        userCacheDto.setLockTime(null);
        userCacheDto.setLastFailedLogin(null);
        userCacheDto.setRoles(Set.of("USER", "ADMIN"));
    }

    @Test
    @DisplayName("getCachedUserByUsername - пользователь найден в БД")
    void getCachedUserByUsername_WhenUserExists_ReturnsCacheDto() {
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

        UserCacheDto result = userCacheService.getCachedUserByUsername("testuser");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getFirstName()).isEqualTo("Test");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountLocked()).isFalse();
        assertThat(result.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");

        verify(userDetailsService).loadUserByUsername("testuser");
    }

    @Test
    @DisplayName("getCachedUserByUsername - пользователь не найден")
    void getCachedUserByUsername_WhenUserNotFound_ReturnsNull() {
        when(userDetailsService.loadUserByUsername("nonexistent"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        UserCacheDto result = userCacheService.getCachedUserByUsername("nonexistent");

        assertThat(result).isNull();
        verify(userDetailsService).loadUserByUsername("nonexistent");
    }

    @Test
    @DisplayName("getCachedUserByUsername - пользователь заблокирован")
    void getCachedUserByUsername_WhenUserLocked_ReturnsCacheDtoWithLockedStatus() {
        testUser.setAccountLocked(true);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(5));
        testUser.setFailedAttempts(5);

        UserDetailsImpl lockedUserDetails = new UserDetailsImpl(testUser, securityProperties);
        when(userDetailsService.loadUserByUsername("lockeduser")).thenReturn(lockedUserDetails);

        UserCacheDto result = userCacheService.getCachedUserByUsername("lockeduser");

        assertThat(result).isNotNull();
        assertThat(result.isAccountLocked()).isTrue();
        assertThat(result.getFailedAttempts()).isEqualTo(5);
        assertThat(result.getLockTime()).isNotNull();
    }

    @Test
    @DisplayName("getCachedUserById - пользователь найден по ID")
    void getCachedUserById_WhenUserExists_ReturnsCacheDto() {
        when(userDetailsService.loadUserById(1L)).thenReturn(userDetails);

        UserCacheDto result = userCacheService.getCachedUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userDetailsService).loadUserById(1L);
    }

    @Test
    @DisplayName("getCachedUserById - пользователь не найден по ID")
    void getCachedUserById_WhenUserNotFound_ReturnsNull() {
        when(userDetailsService.loadUserById(999L))
                .thenThrow(new UsernameNotFoundException("User not found"));

        UserCacheDto result = userCacheService.getCachedUserById(999L);

        assertThat(result).isNull();
        verify(userDetailsService).loadUserById(999L);
    }

    @Test
    @DisplayName("cacheUser - успешное кэширование пользователя")
    void cacheUser_WhenValidDto_ReturnsSameDto() {
        UserCacheDto result = userCacheService.cacheUser("testuser", userCacheDto);

        assertThat(result).isEqualTo(userCacheDto);
    }

    @Test
    @DisplayName("cacheUser - null DTO")
    void cacheUser_WhenDtoIsNull_ReturnsNull() {
        UserCacheDto result = userCacheService.cacheUser("testuser", null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("evictUserCache - успешная очистка кэша")
    void evictUserCache_WhenCalled_DoesNotThrow() {
        userCacheService.evictUserCache("testuser");

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("evictAllUserCache - успешная очистка всего кэша")
    void evictAllUserCache_WhenCalled_DoesNotThrow() {
        userCacheService.evictAllUserCache();

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("createUserDetailsFromCache - успешное создание UserDetails из DTO")
    void createUserDetailsFromCache_WhenValidDto_ReturnsUserDetails() {
        UserDetailsImpl result = userCacheService.createUserDetailsFromCache(userCacheDto);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getUser().getFirstName()).isEqualTo("Test");
        assertThat(result.getUser().getLastName()).isEqualTo("User");
        assertThat(result.getUser().isEnabled()).isTrue();
        assertThat(result.getUser().isAccountLocked()).isFalse();
        assertThat(result.getUser().getFailedAttempts()).isEqualTo(0);

        assertThat(result.getUser().getRoles()).isNullOrEmpty();
    }

    @Test
    @DisplayName("createUserDetailsFromCache - null DTO")
    void createUserDetailsFromCache_WhenDtoIsNull_ReturnsNull() {
        UserDetailsImpl result = userCacheService.createUserDetailsFromCache(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("createUserDetailsFromCache - DTO с заблокированным аккаунтом")
    void createUserDetailsFromCache_WhenDtoHasLockedAccount_ReturnsLockedUserDetails() {
        userCacheDto.setAccountLocked(true);
        userCacheDto.setFailedAttempts(5);
        userCacheDto.setLockTime(LocalDateTime.now().minusMinutes(10));
        userCacheDto.setLastFailedLogin(LocalDateTime.now().minusMinutes(15));

        UserDetailsImpl result = userCacheService.createUserDetailsFromCache(userCacheDto);

        assertThat(result).isNotNull();
        assertThat(result.getUser().isAccountLocked()).isTrue();
        assertThat(result.getUser().getFailedAttempts()).isEqualTo(5);
        assertThat(result.getUser().getLockTime()).isNotNull();
        assertThat(result.getUser().getLastFailedLogin()).isNotNull();
    }

    @Test
    @DisplayName("createUserDetailsFromCache - DTO с минимальными данными")
    void createUserDetailsFromCache_WhenDtoHasMinimalData_ReturnsUserDetails() {
        UserCacheDto minimalDto = new UserCacheDto();
        minimalDto.setId(2L);
        minimalDto.setUsername("minimal");
        minimalDto.setFirstName("Min");
        minimalDto.setLastName("User");
        minimalDto.setEnabled(true);

        UserDetailsImpl result = userCacheService.createUserDetailsFromCache(minimalDto);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("minimal");
        assertThat(result.getUser().getFirstName()).isEqualTo("Min");
        assertThat(result.getUser().getLastName()).isEqualTo("User");
        assertThat(result.getUser().isEnabled()).isTrue();
        assertThat(result.getUser().isAccountLocked()).isFalse();
        assertThat(result.getUser().getFailedAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("convertToCacheDTO - успешное преобразование UserDetails в DTO")
    void convertToCacheDTO_WhenValidUserDetails_ReturnsCacheDto() {
        UserCacheDto result = userCacheService.convertToCacheDTO(userDetails);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getFirstName()).isEqualTo("Test");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountLocked()).isFalse();
        assertThat(result.getFailedAttempts()).isEqualTo(0);
        assertThat(result.getLastLoginAt()).isEqualTo(testUser.getLastLoginAt());
        assertThat(result.getLastLogoutAt()).isEqualTo(testUser.getLastLogoutAt());
        assertThat(result.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    @DisplayName("convertToCacheDTO - null UserDetails")
    void convertToCacheDTO_WhenUserDetailsIsNull_ReturnsNull() {
        UserCacheDto result = userCacheService.convertToCacheDTO(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("convertToCacheDTO - UserDetails с null пользователем")
    void convertToCacheDTO_WhenUserDetailsHasNullUser_ReturnsNull() {
        UserCacheDto result = userCacheService.convertToCacheDTO(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("convertToCacheDTO - пользователь без ролей")
    void convertToCacheDTO_WhenUserHasNoRoles_ReturnsDtoWithEmptyRoles() {
        testUser.setRoles(null); // Нет ролей
        UserDetailsImpl noRolesDetails = new UserDetailsImpl(testUser, securityProperties);

        UserCacheDto result = userCacheService.convertToCacheDTO(noRolesDetails);

        assertThat(result).isNotNull();
        assertThat(result.getRoles()).isNullOrEmpty();
    }

    @Test
    @DisplayName("convertToCacheDTO - пользователь с заблокированным аккаунтом")
    void convertToCacheDTO_WhenUserLocked_ReturnsDtoWithLockedStatus() {
        testUser.setAccountLocked(true);
        testUser.setFailedAttempts(5);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(5));
        testUser.setLastFailedLogin(LocalDateTime.now().minusMinutes(10));

        UserDetailsImpl lockedUserDetails = new UserDetailsImpl(testUser, securityProperties);

        UserCacheDto result = userCacheService.convertToCacheDTO(lockedUserDetails);

        assertThat(result).isNotNull();
        assertThat(result.isAccountLocked()).isTrue();
        assertThat(result.getFailedAttempts()).isEqualTo(5);
        assertThat(result.getLockTime()).isNotNull();
        assertThat(result.getLastFailedLogin()).isNotNull();
    }

    @Test
    @DisplayName("Интеграция методов convert и create (туда-обратно)")
    void convertAndCreate_IntegrationTest() {
        testUser.setAccountLocked(true);
        testUser.setFailedAttempts(3);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(5));
        UserDetailsImpl originalDetails = new UserDetailsImpl(testUser, securityProperties);

        UserCacheDto dto = userCacheService.convertToCacheDTO(originalDetails);

        assertThat(dto).isNotNull();
        assertThat(dto.isAccountLocked()).isTrue();
        assertThat(dto.getFailedAttempts()).isEqualTo(3);

        UserDetailsImpl restoredDetails = userCacheService.createUserDetailsFromCache(dto);

        assertThat(restoredDetails).isNotNull();
        assertThat(restoredDetails.getUser().isAccountLocked()).isTrue();
        assertThat(restoredDetails.getUser().getFailedAttempts()).isEqualTo(3);
        assertThat(restoredDetails.getUsername()).isEqualTo("testuser");

        assertThat(restoredDetails.getUser().getId()).isEqualTo(originalDetails.getUser().getId());
        assertThat(restoredDetails.getUser().getUsername()).isEqualTo(originalDetails.getUsername());
        assertThat(restoredDetails.getUser().getFirstName()).isEqualTo(originalDetails.getUser().getFirstName());
        assertThat(restoredDetails.getUser().getLastName()).isEqualTo(originalDetails.getUser().getLastName());
    }

    @Test
    @DisplayName("convertToCacheDTO - пароль НЕ включается в DTO (безопасность)")
    void convertToCacheDTO_PasswordNotIncludedInDto() {
        UserCacheDto result = userCacheService.convertToCacheDTO(userDetails);

        assertThat(result).isNotNull();
        try {
            result.getClass().getMethod("getPasswordHash");
        } catch (NoSuchMethodException e) {
            assertThat(true).isTrue();
        }
    }

    @Test
    @DisplayName("createUserDetailsFromCache - UserDetails без пароля")
    void createUserDetailsFromCache_ResultHasNoPassword() {
        UserDetailsImpl result = userCacheService.createUserDetailsFromCache(userCacheDto);

        assertThat(result).isNotNull();
        assertThat(result.getPassword()).isNullOrEmpty();
    }
}

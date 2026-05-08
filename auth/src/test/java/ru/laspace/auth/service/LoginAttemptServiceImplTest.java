package ru.laspace.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.laspace.auth.config.SecurityProperties;
import ru.laspace.auth.dto.cache.CachedUser;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.repository.UserRepository;
import ru.laspace.auth.service.impl.LoginAttemptServiceImpl;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCacheService userCacheService;

    @InjectMocks
    private LoginAttemptServiceImpl loginAttemptService;

    private SecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.setMaxFailedAttempts(5);
        securityProperties.setAccountLockDurationMinutes(15);
        securityProperties.setLoginDelayMillis(0); // без задержки в тестах
        loginAttemptService = new LoginAttemptServiceImpl(userRepository, securityProperties, userCacheService);
    }

    @Test
    void loginFailed_incrementsFailedAttempts() {
        User user = buildUser("igor", 0, false, null);
        when(userRepository.findByUsernameInternal("igor")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        loginAttemptService.loginFailed("igor");

        assertThat(user.getFailedAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
        verify(userCacheService).updateCachedUser(user);
    }

    @Test
    void loginFailed_locksAccountWhenMaxAttemptsReached() {
        User user = buildUser("igor", 4, false, null); // 4 попытки, лимит 5
        when(userRepository.findByUsernameInternal("igor")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        loginAttemptService.loginFailed("igor");

        assertThat(user.getFailedAttempts()).isEqualTo(5);
        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.getLockTime()).isNotNull();
    }

    @Test
    void loginFailed_doesNothingForUnknownUser() {
        when(userRepository.findByUsernameInternal("unknown")).thenReturn(Optional.empty());

        loginAttemptService.loginFailed("unknown");

        verify(userRepository, never()).save(any());
        verify(userCacheService, never()).updateCachedUser(any());
    }

    @Test
    void loginFailed_doesNotLockBeforeMaxAttempts() {
        User user = buildUser("igor", 3, false, null);
        when(userRepository.findByUsernameInternal("igor")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        loginAttemptService.loginFailed("igor");

        assertThat(user.isAccountLocked()).isFalse();
    }

    @Test
    void loginSucceeded_resetsFailedAttempts() {
        User user = buildUser("igor", 3, false, null);
        when(userRepository.findByUsernameInternal("igor")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        loginAttemptService.loginSucceeded("igor");

        assertThat(user.getFailedAttempts()).isEqualTo(0);
        assertThat(user.isAccountLocked()).isFalse();
        verify(userCacheService).updateCachedUser(user);
    }

    @Test
    void isAccountLocked_returnsFalseWhenUserNotInCache() {
        when(userCacheService.findByUsername("igor")).thenReturn(Optional.empty());

        boolean result = loginAttemptService.isAccountLocked("igor");

        assertThat(result).isFalse();
    }

    @Test
    void isAccountLocked_returnsTrueWhenLockedAndNotExpired() {
        CachedUser cachedUser = buildCachedUser("igor", true, LocalDateTime.now());
        when(userCacheService.findByUsername("igor")).thenReturn(Optional.of(cachedUser));

        boolean result = loginAttemptService.isAccountLocked("igor");

        assertThat(result).isTrue();
    }

    @Test
    void isAccountLocked_returnsFalseWhenLockExpired() {
        // Блокировка была 30 минут назад, лимит 15 минут — истекла
        CachedUser cachedUser = buildCachedUser("igor", true,
                LocalDateTime.now().minusMinutes(30));
        when(userCacheService.findByUsername("igor")).thenReturn(Optional.of(cachedUser));

        User user = buildUser("igor", 5, true, LocalDateTime.now().minusMinutes(30));
        when(userRepository.findByUsernameInternal("igor")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        boolean result = loginAttemptService.isAccountLocked("igor");

        assertThat(result).isFalse();
        assertThat(user.isAccountLocked()).isFalse();
    }

    @Test
    void isAccountLocked_returnsFalseWhenNotLocked() {
        CachedUser cachedUser = buildCachedUser("igor", false, null);
        when(userCacheService.findByUsername("igor")).thenReturn(Optional.of(cachedUser));

        boolean result = loginAttemptService.isAccountLocked("igor");

        assertThat(result).isFalse();
    }

    @Test
    void getRemainingAttempts_returnsCorrectCount() {
        CachedUser cachedUser = buildCachedUser("igor", false, null);
        cachedUser.setFailedAttempts(3);
        when(userCacheService.findByUsername("igor")).thenReturn(Optional.of(cachedUser));

        int remaining = loginAttemptService.getRemainingAttempts("igor");

        assertThat(remaining).isEqualTo(2); // 5 - 3 = 2
    }

    @Test
    void getRemainingAttempts_returnsMaxWhenUserNotInCache() {
        when(userCacheService.findByUsername("igor")).thenReturn(Optional.empty());

        int remaining = loginAttemptService.getRemainingAttempts("igor");

        assertThat(remaining).isEqualTo(5);
    }

    private User buildUser(String username, int failedAttempts,
            boolean accountLocked, LocalDateTime lockTime) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        user.setFailedAttempts(failedAttempts);
        user.setAccountLocked(accountLocked);
        user.setLockTime(lockTime);
        return user;
    }

    private CachedUser buildCachedUser(String username, boolean accountLocked, LocalDateTime lockTime) {
        CachedUser cachedUser = new CachedUser();
        cachedUser.setId(1L);
        cachedUser.setUsername(username);
        cachedUser.setEnabled(true);
        cachedUser.setFailedAttempts(0);
        cachedUser.setAccountLocked(accountLocked);
        cachedUser.setLockTime(lockTime);
        return cachedUser;
    }
}

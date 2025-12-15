package ru.laspace.spo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.laspace.spo.config.SecurityProperties;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.repository.UserRepository;
import ru.laspace.spo.service.impl.LoginAttemptServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для LoginAttemptServiceImpl")
class LoginAttemptServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityProperties securityProperties;

    @InjectMocks
    private LoginAttemptServiceImpl loginAttemptService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setAccountLocked(false);
        testUser.setFailedAttempts(0);
        testUser.setLockTime(null);
        testUser.setLastFailedLogin(null);
    }

    @Test
    @DisplayName("loginSucceeded - успешный вход сбрасывает счетчик")
    void loginSucceeded_WhenUserExists_ResetsFailedAttempts() {
        // Arrange
        testUser.setFailedAttempts(3);
        testUser.setAccountLocked(true);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(10));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        loginAttemptService.loginSucceeded("testuser");

        verify(userRepository).save(testUser);
        assertThat(testUser.getFailedAttempts()).isEqualTo(0);
        assertThat(testUser.isAccountLocked()).isFalse();
        assertThat(testUser.getLockTime()).isNull();
    }

    @Test
    @DisplayName("loginSucceeded - пользователь не найден (ничего не происходит)")
    void loginSucceeded_WhenUserNotFound_DoesNothing() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        loginAttemptService.loginSucceeded("nonexistent");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("loginSucceeded - пользователь без неудачных попыток")
    void loginSucceeded_WhenUserHasNoFailedAttempts_StillSaves() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        loginAttemptService.loginSucceeded("testuser");

        verify(userRepository).save(testUser);
        assertThat(testUser.getFailedAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("loginFailed - увеличение счетчика попыток (защита включена)")
    void loginFailed_WhenUserExistsAndProtectionEnabled_IncrementsCounter() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(securityProperties.isEnableBruteForceProtection()).thenReturn(true);
        when(securityProperties.getMaxFailedAttempts()).thenReturn(5);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        loginAttemptService.loginFailed("testuser");

        verify(userRepository).save(testUser);
        assertThat(testUser.getFailedAttempts()).isEqualTo(1);
        assertThat(testUser.getLastFailedLogin()).isNotNull();
        assertThat(testUser.isAccountLocked()).isFalse();
    }

    @Test
    @DisplayName("loginFailed - достижение лимита попыток и блокировка")
    void loginFailed_WhenMaxAttemptsReached_LocksAccount() {
        testUser.setFailedAttempts(4);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(securityProperties.isEnableBruteForceProtection()).thenReturn(true);
        when(securityProperties.getMaxFailedAttempts()).thenReturn(5);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        loginAttemptService.loginFailed("testuser");

        verify(userRepository).save(testUser);
        assertThat(testUser.getFailedAttempts()).isEqualTo(5);
        assertThat(testUser.isAccountLocked()).isTrue();
        assertThat(testUser.getLockTime()).isNotNull();
    }

    @Test
    @DisplayName("loginFailed - пользователь уже заблокирован (не блокируем повторно)")
    void loginFailed_WhenUserAlreadyLocked_DoesNotLockAgain() {
        LocalDateTime lockTime = LocalDateTime.now().minusMinutes(5);
        testUser.setFailedAttempts(5);
        testUser.setAccountLocked(true);
        testUser.setLockTime(lockTime);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(securityProperties.isEnableBruteForceProtection()).thenReturn(true);
        when(securityProperties.getMaxFailedAttempts()).thenReturn(5);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        loginAttemptService.loginFailed("testuser");

        verify(userRepository).save(testUser);
        assertThat(testUser.getFailedAttempts()).isEqualTo(6);
        assertThat(testUser.isAccountLocked()).isTrue();
        assertThat(testUser.getLockTime()).isEqualTo(lockTime);
    }

    @Test
    @DisplayName("loginFailed - пользователь не найден (применяется задержка)")
    void loginFailed_WhenUserNotFound_AppliesLoginDelay() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        when(securityProperties.getLoginDelayMillis()).thenReturn(100);

        LoginAttemptServiceImpl spyService = new LoginAttemptServiceImpl(userRepository, securityProperties);

        spyService.loginFailed("nonexistent");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("loginFailed - защита от brute force отключена")
    void loginFailed_WhenProtectionDisabled_DoesNotIncrementCounter() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(securityProperties.isEnableBruteForceProtection()).thenReturn(false);

        loginAttemptService.loginFailed("testuser");

        verify(userRepository, never()).save(any(User.class));
        assertThat(testUser.getFailedAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("loginFailed - блокировка истекла, новая блокировка")
    void loginFailed_WhenLockExpiredAndMaxAttempts_LocksAgain() {
        LocalDateTime expiredLockTime = LocalDateTime.now().minusMinutes(20);
        testUser.setFailedAttempts(4);
        testUser.setAccountLocked(true);
        testUser.setLockTime(expiredLockTime);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(securityProperties.isEnableBruteForceProtection()).thenReturn(true);
        when(securityProperties.getMaxFailedAttempts()).thenReturn(5);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        loginAttemptService.loginFailed("testuser");

        verify(userRepository).save(testUser);
        assertThat(testUser.getFailedAttempts()).isEqualTo(5);
        assertThat(testUser.isAccountLocked()).isTrue();
        assertThat(testUser.getLockTime()).isNotNull();
        assertThat(testUser.getLockTime()).isNotEqualTo(expiredLockTime);
    }

    @Test
    @DisplayName("isAccountLocked - аккаунт заблокирован и блокировка не истекла")
    void isAccountLocked_WhenLockedAndNotExpired_ReturnsTrue() {
        testUser.setAccountLocked(true);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(5));
        testUser.setFailedAttempts(5);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        boolean result = loginAttemptService.isAccountLocked("testuser");

        assertThat(result).isTrue();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("isAccountLocked - аккаунт заблокирован, но блокировка истекла")
    void isAccountLocked_WhenLockedButExpired_ReturnsFalseAndUnlocks() {
        LocalDateTime expiredLockTime = LocalDateTime.now().minusMinutes(20);
        testUser.setAccountLocked(true);
        testUser.setLockTime(expiredLockTime);
        testUser.setFailedAttempts(5);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean result = loginAttemptService.isAccountLocked("testuser");

        assertThat(result).isFalse();
        verify(userRepository).save(testUser);
        assertThat(testUser.isAccountLocked()).isFalse();
        assertThat(testUser.getLockTime()).isNull();
    }

    @Test
    @DisplayName("isAccountLocked - аккаунт не заблокирован")
    void isAccountLocked_WhenNotLocked_ReturnsFalse() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        boolean result = loginAttemptService.isAccountLocked("testuser");

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("isAccountLocked - пользователь не найден")
    void isAccountLocked_WhenUserNotFound_ReturnsFalse() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        boolean result = loginAttemptService.isAccountLocked("nonexistent");

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("getRemainingAttempts - обычный пользователь с попытками")
    void getRemainingAttempts_WhenUserExists_ReturnsRemainingAttempts() {
        testUser.setFailedAttempts(2);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(securityProperties.getMaxFailedAttempts()).thenReturn(5);

        int remaining = loginAttemptService.getRemainingAttempts("testuser");

        assertThat(remaining).isEqualTo(3);
    }

    @Test
    @DisplayName("getRemainingAttempts - пользователь заблокирован")
    void getRemainingAttempts_WhenUserLocked_ReturnsZero() {
        testUser.setFailedAttempts(5);
        testUser.setAccountLocked(true);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(securityProperties.getMaxFailedAttempts()).thenReturn(5);

        int remaining = loginAttemptService.getRemainingAttempts("testuser");

        assertThat(remaining).isEqualTo(0);
    }

    @Test
    @DisplayName("getRemainingAttempts - пользователь не найден")
    void getRemainingAttempts_WhenUserNotFound_ReturnsMaxAttempts() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        when(securityProperties.getMaxFailedAttempts()).thenReturn(5);

        int remaining = loginAttemptService.getRemainingAttempts("nonexistent");

        assertThat(remaining).isEqualTo(5);
    }

    @Test
    @DisplayName("getRemainingAttempts - достигнут лимит попыток, но не заблокирован")
    void getRemainingAttempts_WhenMaxAttemptsButNotLocked_ReturnsZero() {
        testUser.setFailedAttempts(5);
        testUser.setAccountLocked(false);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(securityProperties.getMaxFailedAttempts()).thenReturn(5);

        int remaining = loginAttemptService.getRemainingAttempts("testuser");

        assertThat(remaining).isEqualTo(0);
    }

    @Test
    @DisplayName("getRemainingAttempts - блокировка истекла, но счетчик остался")
    void getRemainingAttempts_WhenLockExpiredButCounterRemains_ReturnsCorrectValue() {
        LocalDateTime expiredLockTime = LocalDateTime.now().minusMinutes(20);
        testUser.setFailedAttempts(5);
        testUser.setAccountLocked(true);
        testUser.setLockTime(expiredLockTime);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(securityProperties.getMaxFailedAttempts()).thenReturn(5);

        int remaining = loginAttemptService.getRemainingAttempts("testuser");

        assertThat(remaining).isEqualTo(0);
    }

    @Test
    @DisplayName("unlockAccount - успешная разблокировка")
    void unlockAccount_WhenUserExists_UnlocksAccount() {
        testUser.setFailedAttempts(5);
        testUser.setAccountLocked(true);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        loginAttemptService.unlockAccount("testuser");

        verify(userRepository).save(testUser);
        assertThat(testUser.getFailedAttempts()).isEqualTo(0);
        assertThat(testUser.isAccountLocked()).isFalse();
        assertThat(testUser.getLockTime()).isNull();
    }

    @Test
    @DisplayName("unlockAccount - пользователь не найден")
    void unlockAccount_WhenUserNotFound_DoesNothing() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        loginAttemptService.unlockAccount("nonexistent");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("applyLoginDelay - задержка больше 0")
    void applyLoginDelay_WhenDelayGreaterThanZero_Sleeps() throws InterruptedException {
        when(securityProperties.getLoginDelayMillis()).thenReturn(50);

        long startTime = System.currentTimeMillis();

        loginAttemptService.applyLoginDelay();

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        assertThat(elapsed).isGreaterThanOrEqualTo(45);
    }

    @Test
    @DisplayName("applyLoginDelay - задержка равна 0")
    void applyLoginDelay_WhenDelayZero_DoesNotSleep() throws InterruptedException {
        when(securityProperties.getLoginDelayMillis()).thenReturn(0);

        long startTime = System.currentTimeMillis();

        loginAttemptService.applyLoginDelay();

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        assertThat(elapsed).isLessThan(10);
    }

    @Test
    @DisplayName("applyLoginDelay - прерывание потока")
    void applyLoginDelay_WhenInterrupted_InterruptsThread() {
        when(securityProperties.getLoginDelayMillis()).thenReturn(1000);

        Thread testThread = new Thread(() -> {
            loginAttemptService.applyLoginDelay();
        });

        testThread.start();
        testThread.interrupt();

        try {
            testThread.join(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
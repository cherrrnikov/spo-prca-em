package ru.laspace.spo.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.config.SecurityProperties;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.repository.UserRepository;
import ru.laspace.spo.service.LoginAttemptService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {
    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    @Transactional
    @Override
    public void loginSucceeded(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int oldAttempts = user.getFailedAttempts();
            boolean wasLocked = user.isAccountLocked();

            user.resetFailedAttempts();
            User savedUser = userRepository.save(user); // Явное сохранение

            log.info("Сброс счетчика неудачных попыток для пользователя: {}. Было: {}, стало: {}, была блокировка: {}",
                    username, oldAttempts, savedUser.getFailedAttempts(), wasLocked);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // Новая транзакция для каждого неудачного входа
    @Override
    public void loginFailed(String username) {
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            log.debug("Пользователь не найден: {}. Применяем задержку.", username);
            applyLoginDelay();
            return;
        }

        if (!securityProperties.isEnableBruteForceProtection()) {
            log.debug("Защита от brute force отключена для пользователя: {}", username);
            return;
        }

        // Сохраняем старое состояние для логов
        int oldAttempts = user.getFailedAttempts();
        boolean wasLocked = user.isAccountLocked();

        // Увеличиваем счетчик
        user.incrementFailedAttempts();

        log.debug("Увеличиваем счетчик для пользователя: {}. Было: {}, стало: {}",
                username, oldAttempts, user.getFailedAttempts());

        if (user.getFailedAttempts() >= securityProperties.getMaxFailedAttempts()) {
            if (!user.isAccountLocked() || user.isAccountLockExpired()) {
                user.lockAccount();
                log.warn("Аккаунт заблокирован из-за слишком большого количества неудачных попыток: {}. Попыток: {}",
                        username, user.getFailedAttempts());
            }
        }

        User savedUser = userRepository.save(user); // Явное сохранение

        log.info("Неудачная попытка входа для пользователя: {}. Попытки: {} -> {}. Заблокирован: {}",
                username, oldAttempts, savedUser.getFailedAttempts(), savedUser.isAccountLocked());
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isAccountLocked(String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    boolean isLocked = user.isAccountLocked() && !user.isAccountLockExpired();

                    if (isLocked) {
                        log.debug("Аккаунт заблокирован: {}. Попыток: {}, время блокировки: {}",
                                username, user.getFailedAttempts(), user.getLockTime());
                    }

                    // Если блокировка истекла, снимаем её
                    if (user.isAccountLocked() && user.isAccountLockExpired()) {
                        log.info("Блокировка истекла для пользователя: {}. Снимаем блокировку.", username);
                        user.setAccountLocked(false);
                        user.setLockTime(null);
                        userRepository.save(user);
                    }
                    return isLocked;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    @Override
    public int getRemainingAttempts(String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    int remaining = securityProperties.getMaxFailedAttempts() - user.getFailedAttempts();

                    if (user.isAccountLocked() && !user.isAccountLockExpired()) {
                        remaining = 0;
                    }

                    remaining = Math.max(0, remaining);

                    log.debug("Оставшихся попыток для пользователя {}: {} (использовано: {}, максимум: {})",
                            username, remaining, user.getFailedAttempts(), securityProperties.getMaxFailedAttempts());
                    return remaining;
                })
                .orElse(securityProperties.getMaxFailedAttempts());
    }

    @Transactional
    @Override
    public void unlockAccount(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int oldAttempts = user.getFailedAttempts();
            user.resetFailedAttempts();
            User savedUser = userRepository.save(user);
            log.info("Аккаунт разблокирован: {}. Было попыток: {}, стало: {}",
                    username, oldAttempts, savedUser.getFailedAttempts());
        });
    }

    // Метод для задержки при неудачной попытке (защита от брутфорса)
    @Override
    public void applyLoginDelay() {
        if (securityProperties.getLoginDelayMillis() > 0) {
            try {
                log.debug("Применяем задержку в {} мс", securityProperties.getLoginDelayMillis());
                Thread.sleep(securityProperties.getLoginDelayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Задержка прервана");
            }
        }
    }
}
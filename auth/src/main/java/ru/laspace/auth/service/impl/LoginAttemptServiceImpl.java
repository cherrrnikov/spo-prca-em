package ru.laspace.auth.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.config.SecurityProperties;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.repository.UserRepository;
import ru.laspace.auth.service.LoginAttemptService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    @Override
    @Transactional
    public void loginSucceeded(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.resetFailedAttempts();
            userRepository.save(user);
            log.debug("Reset failed attempts for user: {}", username);
        });
    }

    @Override
    @Transactional
    public void loginFailed(String username) {
        applyLoginDelay();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        user.incrementFailedAttempts();

        if (user.getFailedAttempts() >= securityProperties.getMaxFailedAttempts()) {
            user.lockAccount();
            log.warn("Account locked for user: {}", username);
        }

        userRepository.save(user);
        log.debug("Failed attempt for user: {}, attempts: {}",
                username, user.getFailedAttempts());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountLocked(String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    if (user.isAccountLocked() &&
                            !securityProperties.isLockExpired(user.getLockTime())) {
                        return true;
                    }
                    if (user.isAccountLocked()) {
                        user.setAccountLocked(false);
                        user.setLockTime(null);
                        userRepository.save(user);
                    }
                    return false;
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public int getRemainingAttempts(String username) {
        return userRepository.findByUsername(username)
                .map(user -> Math.max(0,
                        securityProperties.getMaxFailedAttempts() - user.getFailedAttempts()))
                .orElse(securityProperties.getMaxFailedAttempts());
    }

    @Override
    @Transactional
    public void unlockAccount(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setAccountLocked(false);
            user.setLockTime(null);
            user.setFailedAttempts(0);
            userRepository.save(user);
            log.info("Account unlocked for user: {}", username);
        });
    }

    @Override
    public void applyLoginDelay() {
        if (securityProperties.getLoginDelayMillis() > 0) {
            try {
                Thread.sleep(securityProperties.getLoginDelayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
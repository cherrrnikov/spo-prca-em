package ru.laspace.spo.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.exception.AuthException;
import ru.laspace.spo.exception.NotFoundException;
import ru.laspace.spo.repository.UserRepository;
import ru.laspace.spo.service.AuthService;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User authenticate(String username, String password) {
        log.info("Попытка аутентификации: {}", username);

        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            log.warn("Пользователь не найден: {}", username);
            return new AuthException("Неверный логин или пароль");
        });

        if (!user.isEnabled()) {
            log.warn("Аккаунт отключен, ID: {}", user.getId());
            throw new AuthException("Аккаунт отключен");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            log.warn("У пользователя не установлен пароль, ID: {}", user.getId());
            throw new AuthException("Пароль не установлен");
        }

        boolean passwordValid = passwordEncoder.matches(password, user.getPasswordHash());

        if (!passwordValid) {
            log.warn("Неверный пароль, ID: {}", user.getId());
            throw new AuthException("Неверный пароль");
        }

        user.setLastLoginAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        log.info("Пользователь успешно аутентифицирован, ID: {}, username: {}", updatedUser.getId(),
                updatedUser.getUsername());

        return updatedUser;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @SuppressWarnings("null")
    @Override
    public User findById(Long userId) {
        log.debug("Поиск пользователя по ID: {}", userId);

        return userRepository.findById(userId).orElseThrow(() -> {
            log.warn("Пользователь не найден по ID: {}", userId);
            return new NotFoundException("Пользователь не найден");
        });
    }
}

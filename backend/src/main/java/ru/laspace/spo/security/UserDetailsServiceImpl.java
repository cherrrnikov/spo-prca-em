package ru.laspace.spo.security;

import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.config.SecurityProperties;
import ru.laspace.spo.entity.Role;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.repository.UserRepository;
import ru.laspace.spo.service.LoginAttemptService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;
    private final LoginAttemptService loginAttemptService;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (securityProperties.isEnableBruteForceProtection() &&
                loginAttemptService.isAccountLocked(username)) {
            log.warn("Попытка входа в заблокированный аккаунт: {}", username);
            throw new UsernameNotFoundException("Аккаунт заблокирован. Попробуйте позже.");
        }

        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            log.warn("Пользователь не найден: {}", username);

            // Увеличиваем счетчик неудачных попыток даже для несуществующего пользователя
            // (чтобы не раскрывать информацию о существовании пользователя)
            if (securityProperties.isEnableBruteForceProtection()) {
                loginAttemptService.applyLoginDelay();
            }

            return new UsernameNotFoundException("Пользователь не найден");
        });

        if (!user.isEnabled()) {
            log.warn("Аккаунт отключен, ID: {}", user.getId());

            if (securityProperties.isEnableBruteForceProtection()) {
                loginAttemptService.applyLoginDelay();
            }

            throw new UsernameNotFoundException("Аккаунт отключен.");
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(user, securityProperties);

        if (!userDetails.isAccountNonLocked()) {
            log.warn("Аккаунт заблокирован: {}", username);
            int remainingTime = securityProperties.getAccountLockDurationMinutes() -
                    (int) java.time.Duration.between(user.getLockTime(), java.time.LocalDateTime.now()).toMinutes();
            throw new UsernameNotFoundException(
                    String.format("Аккаунт заблокирован. Попробуйте через %d минут", Math.max(1, remainingTime)));
        }

        if (user.getRoles() != null) {
            user.getRoles().size(); // Это загрузит роли
            log.debug("Роли загружены: {}",
                    user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        } else {
            log.warn("Роли не загружены или null!");
        }

        return userDetails;
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("Пользователь не найден по ID: {}", userId);
            return new UsernameNotFoundException("Пользователь не найден");
        });

        if (!user.isEnabled()) {
            log.warn("Аккаунт отключен, ID: {}", userId);
            throw new UsernameNotFoundException("Аккаунт отключен.");
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(user, securityProperties);

        if (!userDetails.isAccountNonLocked()) {
            log.warn("Аккаунт заблокирован, ID: {}", userId);
            throw new UsernameNotFoundException("Аккаунт заблокирован");
        }

        if (user.getRoles() != null) {
            user.getRoles().size(); // Это загрузит роли
            log.debug("Роли загружены для ID {}: {}",
                    userId, user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        }

        return userDetails;
    }
}
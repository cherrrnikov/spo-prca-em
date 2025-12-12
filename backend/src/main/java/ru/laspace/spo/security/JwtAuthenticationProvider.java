package ru.laspace.spo.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.service.UserCacheService;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider {

    private final JwtParser jwtParser;
    private final JwtValidator jwtValidator;
    private final UserCacheService userCacheService;

    public Authentication getAuthentication(String token) {
        if (!jwtValidator.validateToken(token)) {
            log.warn("Попытка создания аутентификации по невалидному токену");
            throw new UsernameNotFoundException("Невалидный токен");
        }

        String username = jwtParser.getUsername(token);

        UserDetails userDetails = userCacheService.getUserByUsername(username);

        if (userDetails == null) {
            log.warn("Пользователь не найден в кэше: {}", username);
            throw new UsernameNotFoundException("Пользователь не найден");
        }

        if (!userDetails.isEnabled()) {
            log.warn("Попытка аутентификации отключенного пользователя: {}", username);
            throw new UsernameNotFoundException("Аккаунт отключен");
        }

        var authorities = jwtParser.getAuthorities(token);

        // Кэшируем связку токен-пользователь для последующих запросов
        userCacheService.getUserFromToken(token, userDetails);

        log.debug("Создана аутентификация для пользователя: {}", username);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                token,
                authorities);
    }

    public Authentication getAuthenticationFromUserDetails(UserDetails userDetails, String token) {
        var authorities = jwtParser.getAuthorities(token);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                token,
                authorities);
    }

    public boolean canCreateAuthentication(String token) {
        if (!jwtValidator.validateToken(token)) {
            return false;
        }

        try {
            String username = jwtParser.getUsername(token);
            UserDetails userDetails = userCacheService.getUserByUsername(username);
            return userDetails != null && userDetails.isEnabled();
        } catch (Exception e) {
            log.debug("Не удалось создать аутентификацию: {}", e.getMessage());
            return false;
        }
    }
}
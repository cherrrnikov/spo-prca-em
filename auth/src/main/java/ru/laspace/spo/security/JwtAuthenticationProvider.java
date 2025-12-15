package ru.laspace.spo.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.dto.cache.UserCacheDto;
import ru.laspace.spo.service.UserCacheService;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider {

    private final JwtParser jwtParser;
    private final JwtValidator jwtValidator;
    private final UserCacheService userCacheService;
    private final UserDetailsServiceImpl userDetailsService;

    public Authentication getAuthentication(String token) {
        if (!jwtValidator.validateToken(token)) {
            log.warn("Попытка создания аутентификации по невалидному токену");
            throw new UsernameNotFoundException("Невалидный токен");
        }

        String username = jwtParser.getUsername(token);

        UserCacheDto cachedUser = userCacheService.getCachedUserByUsername(username);
        UserDetails userDetails;

        if (cachedUser != null) {
            userDetails = userCacheService.createUserDetailsFromCache(cachedUser);
            log.debug("Пользователь загружен из кэша (DTO): {}", username);
        } else {
            userDetails = userDetailsService.loadUserByUsername(username);
            UserCacheDto newCacheDTO = userCacheService.convertToCacheDTO((UserDetailsImpl) userDetails);
            userCacheService.cacheUser(username, newCacheDTO);
            log.debug("Пользователь загружен из БД и закэширован: {}", username);
        }

        if (!userDetails.isEnabled()) {
            log.warn("Попытка аутентификации отключенного пользователя: {}", username);
            throw new UsernameNotFoundException("Аккаунт отключен");
        }

        var authorities = jwtParser.getAuthorities(token);

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
}
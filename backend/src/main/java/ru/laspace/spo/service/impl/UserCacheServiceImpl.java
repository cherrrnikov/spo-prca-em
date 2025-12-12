package ru.laspace.spo.service.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.security.UserDetailsServiceImpl;
import ru.laspace.spo.service.UserCacheService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheServiceImpl implements UserCacheService {

    private final UserDetailsServiceImpl userDetailsService;

    @Cacheable(value = "users", key = "#username", unless = "#result == null")
    @Override
    public UserDetails getUserByUsername(String username) {
        log.debug("Загрузка пользователя из БД для кэширования: {}", username);
        try {
            return userDetailsService.loadUserByUsername(username);
        } catch (Exception e) {
            log.warn("Не удалось загрузить пользователя для кэширования: {}", username, e);
            return null;
        }
    }

    @Cacheable(value = "userDetailsById", key = "#userId", unless = "#result == null")
    @Override
    public UserDetails getUserById(Long userId) {
        log.debug("Загрузка пользователя по ID для кэширования: {}", userId);
        try {
            return userDetailsService.loadUserById(userId);
        } catch (UsernameNotFoundException e) {
            log.warn("Не удалось загрузить пользователя по ID для кэширования: {}", userId, e);
            return null;
        }
    }

    @CachePut(value = "users", key = "#username")
    @Override
    public UserDetails updateUserCache(String username, UserDetails userDetails) {
        log.debug("Обновление кэша пользователя: {}", username);
        return userDetails;
    }

    @CacheEvict(value = { "users", "userDetailsById" }, allEntries = false, key = "#username")
    @Override
    public void evictUserCache(String username) {
        log.debug("Очистка кэша пользователя: {}", username);
    }

    @CacheEvict(value = { "users", "userDetailsById" }, allEntries = true)
    @Override
    public void evictAllUserCache() {
        log.debug("Очистка всего кэша пользователей");
    }

    // Кэширование по токену
    @Cacheable(value = "jwtUsers", key = "#token", unless = "#result == null")
    @Override
    public UserDetails getUserFromToken(String token, UserDetails userDetails) {
        log.debug("Кэширование пользователя по токену");
        return userDetails;
    }

    @Cacheable(value = "jwtUsers", key = "#token", unless = "#result == null")
    public UserDetails getUserFromToken(String token) {
        log.debug("Получение пользователя из кэша по токену: {}", token);
        return null; // Для прямого получения по токену
    }

    @CacheEvict(value = "jwtUsers", key = "#token")
    @Override
    public void evictTokenCache(String token) {
        log.debug("Очистка кэша токена: {}", token);
    }
}
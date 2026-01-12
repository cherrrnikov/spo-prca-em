package ru.laspace.auth.service.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.config.SecurityProperties;
import ru.laspace.auth.dto.cache.UserCacheDto;
import ru.laspace.auth.entity.Role;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.security.UserDetailsImpl;
import ru.laspace.auth.security.UserDetailsServiceImpl;
import ru.laspace.auth.service.UserCacheService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheServiceImpl implements UserCacheService {

    private final UserDetailsServiceImpl userDetailsService;
    private final SecurityProperties securityProperties;

    @Cacheable(value = "users", key = "#username", unless = "#result == null")
    @Override
    public UserCacheDto getCachedUserByUsername(String username) {
        log.debug("Загрузка пользователя из БД для кэширования (DTO): {}", username);
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UserDetailsImpl userDetailsImpl = (UserDetailsImpl) userDetails;
            return convertToCacheDTO(userDetailsImpl);
        } catch (UsernameNotFoundException e) {
            log.warn("Не удалось загрузить пользователя для кэширования: {}", username, e);
            return null;
        }
    }

    @Cacheable(value = "userDetailsById", key = "#userId", unless = "#result == null")
    @Override
    public UserCacheDto getCachedUserById(Long userId) {
        log.debug("Загрузка пользователя по ID для кэширования (DTO): {}", userId);
        try {
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            UserDetailsImpl userDetailsImpl = (UserDetailsImpl) userDetails;

            return convertToCacheDTO(userDetailsImpl);
        } catch (UsernameNotFoundException e) {
            log.warn("Не удалось загрузить пользователя по ID для кэширования: {}", userId, e);
            return null;
        }
    }

    @CachePut(value = "users", key = "#username")
    @Override
    public UserCacheDto cacheUser(String username, UserCacheDto userCacheDTO) {
        log.debug("Обновление кэша пользователя: {}", username);
        return userCacheDTO;
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

    @Override
    public UserDetailsImpl createUserDetailsFromCache(UserCacheDto dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setLastLoginAt(dto.getLastLoginAt());
        user.setLastLogoutAt(dto.getLastLogoutAt());
        user.setEnabled(dto.isEnabled());
        user.setFailedAttempts(dto.getFailedAttempts());
        user.setAccountLocked(dto.isAccountLocked());
        user.setLockTime(dto.getLockTime());
        user.setLastFailedLogin(dto.getLastFailedLogin());

        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Set<Role> roles = dto.getRoles().stream()
                    .map(roleName -> {
                        Role role = new Role();
                        role.setName(roleName);
                        return role;
                    })
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        return new UserDetailsImpl(user, securityProperties);
    }

    @Override
    public UserCacheDto convertToCacheDTO(UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return null;
        }

        User user = userDetails.getUser();
        UserCacheDto dto = new UserCacheDto();

        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setLastLogoutAt(user.getLastLogoutAt());
        dto.setEnabled(user.isEnabled());
        dto.setFailedAttempts(user.getFailedAttempts());
        dto.setAccountLocked(user.isAccountLocked());
        dto.setLockTime(user.getLockTime());
        dto.setLastFailedLogin(user.getLastFailedLogin());

        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet()));
        }

        return dto;
    }
}
package ru.laspace.auth.service.impl;

import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.dto.cache.CachedUser;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.repository.UserRepository;
import ru.laspace.auth.service.UserCacheService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheServiceImpl implements UserCacheService {
    private final UserRepository userRepository;

    @Cacheable(value = "users", key = "#username", unless = "#result == null")
    @Override
    public Optional<CachedUser> findByUsername(String username) {
        log.debug("Cache miss for user: {}", username);

        return userRepository.findByUsernameInternal(username)
                .map(CachedUser::fromEntity);
    }

    @CachePut(value = "users", key = "#user.username")
    @Override
    public CachedUser updateCachedUser(User user) {
        log.debug("Updating cache for user: {}", user.getUsername());
        return CachedUser.fromEntity(user);
    }

    @CacheEvict(value = "users", key = "#username")
    @Override
    public void evictUserFromCache(String username) {
        log.debug("Evicting user from cache: {}", username);
    }

    @Override
    public User toEntityWithRoles(CachedUser cachedUser) {
        if (cachedUser == null) {
            return null;
        }

        User user = cachedUser.toEntity();

        if (cachedUser.getRoles() != null && !cachedUser.getRoles().isEmpty()) {
            user.setRoles(userRepository.findRolesByNames(cachedUser.getRoles()));
        }

        return user;
    }

}

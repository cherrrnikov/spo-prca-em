package ru.laspace.auth.service;

import java.util.Optional;

import ru.laspace.auth.dto.cache.CachedUser;
import ru.laspace.auth.entity.User;

public interface UserCacheService {
    Optional<CachedUser> findByUsername(String username);

    CachedUser updateCachedUser(User user);

    void evictUserFromCache(String username);

    User toEntityWithRoles(CachedUser cachedUser);
}

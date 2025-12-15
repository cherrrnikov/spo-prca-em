package ru.laspace.auth.service;

import ru.laspace.auth.dto.cache.UserCacheDto;
import ru.laspace.auth.security.UserDetailsImpl;

public interface UserCacheService {
    UserCacheDto getCachedUserByUsername(String username);

    UserCacheDto getCachedUserById(Long userId);

    UserCacheDto cacheUser(String username, UserCacheDto userCacheDTO);

    void evictUserCache(String username);

    void evictAllUserCache();

    UserDetailsImpl createUserDetailsFromCache(UserCacheDto dto);

    UserCacheDto convertToCacheDTO(UserDetailsImpl userDetails);
}

package ru.laspace.spo.service;

import ru.laspace.spo.dto.cache.UserCacheDto;
import ru.laspace.spo.security.UserDetailsImpl;

public interface UserCacheService {
    UserCacheDto getCachedUserByUsername(String username);

    UserCacheDto getCachedUserById(Long userId);

    UserCacheDto cacheUser(String username, UserCacheDto userCacheDTO);

    void evictUserCache(String username);

    void evictAllUserCache();

    UserDetailsImpl createUserDetailsFromCache(UserCacheDto dto);

    UserCacheDto convertToCacheDTO(UserDetailsImpl userDetails);
}

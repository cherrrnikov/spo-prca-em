package ru.laspace.spo.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserCacheService {
    UserDetails getUserByUsername(String username);

    UserDetails getUserById(Long userId);

    UserDetails updateUserCache(String username, UserDetails userDetails);

    void evictUserCache(String username);

    void evictAllUserCache();

    UserDetails getUserFromToken(String token, UserDetails userDetails);

    void evictTokenCache(String token);
}

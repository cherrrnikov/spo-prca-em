package ru.laspace.auth.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.service.UserCacheService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserCacheService userCacheService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userCacheService.findByUsername(username)
                .map(cachedUser -> {
                    log.debug("User found in cache: {}", username);
                    return userCacheService.toEntityWithRoles(cachedUser);
                })
                .orElseThrow(() -> {
                    log.debug("User not found in cache or database: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        log.debug("User loaded: {} with {} roles", username, user.getRoles().size());

        return new CustomUserDetails(user);
    }
}
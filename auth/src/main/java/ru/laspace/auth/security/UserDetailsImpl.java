package ru.laspace.auth.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.laspace.auth.config.SecurityProperties;
import ru.laspace.auth.entity.User;

@Getter
@RequiredArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private final User user;
    private final SecurityProperties securityProperties;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (user.isAccountLocked()) {
            if (user.isAccountLockExpired()) {
                user.setAccountLocked(false);
                user.setLockTime(null);
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public Long getId() {
        return user.getId();
    }

    public int getFailedAttempts() {
        return user.getFailedAttempts();
    }

    public LocalDateTime getLockTime() {
        return user.getLockTime();
    }

    public LocalDateTime getLastFailedLogin() {
        return user.getLastFailedLogin();
    }

    public int getRemainingAttempts() {
        return Math.max(0, securityProperties.getMaxFailedAttempts() - user.getFailedAttempts());
    }

    public LocalDateTime getLockExpiryTime() {
        if (user.getLockTime() == null) {
            return null;
        }
        return user.getLockTime().plusMinutes(securityProperties.getAccountLockDurationMinutes());
    }
}

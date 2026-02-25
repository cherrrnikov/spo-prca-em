package ru.laspace.auth.dto.cache;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.laspace.auth.entity.Role;
import ru.laspace.auth.entity.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CachedUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastLogoutAt;
    private boolean enabled;
    private int failedAttempts;
    private boolean accountLocked;
    private LocalDateTime lockTime;
    private Set<String> roles = new HashSet<>();

    public static CachedUser fromEntity(User user) {
        if (user == null) {
            return null;
        }

        CachedUser cachedUser = new CachedUser();
        cachedUser.setId(user.getId());
        cachedUser.setUsername(user.getUsername());
        cachedUser.setPasswordHash(user.getPasswordHash());
        cachedUser.setFirstName(user.getFirstName());
        cachedUser.setLastName(user.getLastName());
        cachedUser.setLastLoginAt(user.getLastLoginAt());
        cachedUser.setLastLogoutAt(user.getLastLogoutAt());
        cachedUser.setEnabled(user.isEnabled());
        cachedUser.setFailedAttempts(user.getFailedAttempts());
        cachedUser.setAccountLocked(user.isAccountLocked());
        cachedUser.setLockTime(user.getLockTime());

        if (user.getRoles() != null) {
            cachedUser.setRoles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet()));
        }

        return cachedUser;
    }

    public User toEntity() {
        User user = new User();
        user.setId(this.id);
        user.setUsername(this.username);
        user.setPasswordHash(this.passwordHash);
        user.setFirstName(this.firstName);
        user.setLastName(this.lastName);
        user.setLastLoginAt(this.lastLoginAt);
        user.setLastLogoutAt(this.lastLogoutAt);
        user.setEnabled(this.enabled);
        user.setFailedAttempts(this.failedAttempts);
        user.setAccountLocked(this.accountLocked);
        user.setLockTime(this.lockTime);
        return user;
    }

}
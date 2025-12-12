package ru.laspace.spo.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    // private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private boolean accountLocked;
    private int failedAttempts;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastLogoutAt;
    private Set<String> roles;
}
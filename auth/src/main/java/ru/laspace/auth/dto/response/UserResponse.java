package ru.laspace.auth.dto.response;

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
    private String username;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private boolean accountLocked;
    private int failedAttempts;
    private Set<String> roles;
}
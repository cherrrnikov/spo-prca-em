package ru.laspace.spo.mapper;

import java.util.HashSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ru.laspace.spo.dto.response.JwtResponse;
import ru.laspace.spo.dto.response.UserResponse;
import ru.laspace.spo.entity.Role;
import ru.laspace.spo.entity.User;

@Component
public class UserMapper {
        public UserResponse toResponse(User user) {
                return UserResponse.builder()
                                .username(user.getUsername())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .enabled(user.isEnabled())
                                .accountLocked(user.isAccountLocked())
                                .failedAttempts(user.getFailedAttempts())
                                .lastLoginAt(user.getLastLoginAt())
                                .lastLogoutAt(user.getLastLogoutAt())
                                .roles(user.getRoles() != null
                                                ? user.getRoles().stream()
                                                                .map(Role::getName)
                                                                .collect(Collectors.toSet())
                                                : new HashSet<>())
                                .build();
        }

        public JwtResponse toJwtResponse(String accessToken, String refreshToken, User user) {
                return JwtResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .username(user.getUsername())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .roles(user.getRoles() != null
                                                ? user.getRoles().stream()
                                                                .map(Role::getName)
                                                                .collect(Collectors.toSet())
                                                : new HashSet<>())
                                .lastLoginAt(user.getLastLoginAt())
                                .build();
        }
}

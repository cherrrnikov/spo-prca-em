package ru.laspace.spo.service;

import ru.laspace.spo.dto.request.LoginRequest;
import ru.laspace.spo.dto.request.RefreshTokenRequest;
import ru.laspace.spo.dto.response.JwtResponse;
import ru.laspace.spo.entity.User;

public interface AuthService {
    User authenticate(String username, String rawPassword);

    User findById(Long userId);

    boolean existsByUsername(String username);

    JwtResponse login(LoginRequest loginRequest);

    JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    void logout(String refreshToken, Long userId);
}

package ru.laspace.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.laspace.auth.dto.request.LoginRequest;
import ru.laspace.auth.dto.request.RefreshTokenRequest;
import ru.laspace.auth.dto.response.JwtResponse;

public interface AuthService {
    JwtResponse login(LoginRequest request, HttpServletRequest httpRequest);

    JwtResponse refresh(RefreshTokenRequest request);

    void logout(String refreshToken);
}

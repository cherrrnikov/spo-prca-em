package ru.laspace.spo.service;

import ru.laspace.spo.dto.request.LoginRequest;
import ru.laspace.spo.dto.request.RefreshTokenRequest;
import ru.laspace.spo.dto.response.JwtResponse;

public interface AuthService {

    JwtResponse login(LoginRequest loginRequest);

    JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    void logout(String refreshToken);
}

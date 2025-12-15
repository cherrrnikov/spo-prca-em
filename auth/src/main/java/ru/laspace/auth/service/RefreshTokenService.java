package ru.laspace.auth.service;

import ru.laspace.auth.entity.RefreshToken;
import ru.laspace.auth.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user, String token);

    RefreshToken verifyRefreshToken(String token);

    void revokeRefreshToken(String token, Long userId);
}

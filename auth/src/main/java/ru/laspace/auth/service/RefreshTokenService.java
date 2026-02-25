package ru.laspace.auth.service;

import ru.laspace.auth.entity.RefreshToken;
import ru.laspace.auth.entity.User;

public interface RefreshTokenService {
    String createRefreshToken(User user, String ipAddress, String userAgent);

    RefreshToken verifyRefreshToken(String token);

    void revokeRefreshToken(String token);

    void revokeAllUserTokens(Long userId);

    void cleanupExpiredTokens();
}

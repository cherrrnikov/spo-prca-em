package ru.laspace.spo.service;

import ru.laspace.spo.entity.RefreshToken;
import ru.laspace.spo.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user, String token);

    RefreshToken verifyRefreshToken(String token);

    void revokeRefreshToken(String token, Long userId);
}

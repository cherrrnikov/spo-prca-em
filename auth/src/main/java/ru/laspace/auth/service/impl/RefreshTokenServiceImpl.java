package ru.laspace.auth.service.impl;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.entity.RefreshToken;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.exception.AuthException;
import ru.laspace.auth.repository.RefreshTokenRepository;
import ru.laspace.auth.security.JwtService;
import ru.laspace.auth.service.RefreshTokenService;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public String createRefreshToken(User user, String ipAddress, String userAgent) {
        // Отзываем ВСЕ активные токены пользователя
        refreshTokenRepository.revokeAllUserTokens(user.getId(), LocalDateTime.now());

        String tokenValue = jwtService.generateRefreshToken(user.getUsername(), user.getId());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        if (!jwtService.validateToken(token) ||
                !jwtService.validateTokenType(token, "refresh")) {
            throw new AuthException("Invalid refresh token");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthException("Refresh token not found"));

        if (!refreshToken.isValid()) {
            throw new AuthException("Refresh token is revoked or expired");
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(tokenEntity -> {
            tokenEntity.revoke();
            refreshTokenRepository.save(tokenEntity);
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthAgo = now.minusMonths(1);

        int expired = refreshTokenRepository.deleteExpiredTokens(now);
        int oldRevoked = refreshTokenRepository.deleteOldRevokedTokens(monthAgo);

        if (expired > 0 || oldRevoked > 0) {
            log.info("Cleaned up {} expired and {} old revoked tokens", expired, oldRevoked);
        }
    }
}
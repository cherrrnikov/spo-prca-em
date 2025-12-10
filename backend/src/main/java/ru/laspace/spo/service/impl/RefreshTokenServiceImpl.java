package ru.laspace.spo.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.entity.RefreshToken;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.exception.TokenRefreshException;
import ru.laspace.spo.repository.RefreshTokenRepository;
import ru.laspace.spo.service.RefreshTokenService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public RefreshToken createRefreshToken(User user, String token) {
        RefreshToken refreshTokenEntity = new RefreshToken();

        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setToken(token);
        refreshTokenEntity.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenEntity.setCreatedAt(LocalDateTime.now());
        refreshTokenEntity.setRevoked(false);

        return refreshTokenEntity;
    }

    @Override
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException("RefreshToken не найден"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new TokenRefreshException("RefreshToken истёк");
        }

        if (refreshToken.isRevoked()) {
            throw new TokenRefreshException("RefreshToken аннулирован");
        }

        return refreshToken;
    }

    @Override
    public void revokeRefreshToken(String refreshToken, Long userId) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    if (token.getUser().getId().equals(userId)) {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                        log.info("Refresh token отозван для пользователя ID={}", userId);
                    } else {
                        log.warn(
                                "Попытка отозвать чужой refreshToken. Токен принадлежит пользователю ID={}, запросил ID={}",
                                token.getUser().getId(), userId);
                        throw new TokenRefreshException("Refresh token не принадлежит пользователю");
                    }
                });
    }
}

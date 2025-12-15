package ru.laspace.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.config.JwtProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtValidator {
    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration().before(new Date())) {
                log.debug("JWT токен истек");
                return false;
            }

            if (jwtProperties.getIssuer() != null && !jwtProperties.getIssuer().isEmpty()) {
                String issuer = claims.getIssuer();
                if (issuer == null || !issuer.equals(jwtProperties.getIssuer())) {
                    log.debug("Неверный issuer токена. Ожидался: {}, получен: {}",
                            jwtProperties.getIssuer(), issuer);
                    return false;
                }
            }

            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT токен истек");
            return false;
        } catch (JwtException e) {
            log.debug("Невалидный JWT токен: {}", e.getClass().getSimpleName());
            return false;
        } catch (Exception e) {
            log.error("Ошибка при валидации токена: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.debug("Не удалось проверить истечение токена: {}", e.getMessage());
            return true; // Если не можем проверить, считаем токен невалидным
        }
    }

    public boolean validateTokenStructure(String token) {
        try {
            // Простая проверка структуры без полного парсинга
            if (token == null || token.split("\\.").length != 3) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
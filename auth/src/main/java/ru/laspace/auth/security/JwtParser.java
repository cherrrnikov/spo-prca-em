package ru.laspace.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.config.JwtProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtParser {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException e) {
            log.warn("Не удалось получить username из токена: {}", e.getMessage());
            throw new JwtException("Невалидный токен");
        }
    }

    public Long getUserId(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("userId", Long.class);
        } catch (JwtException e) {
            log.warn("Не удалось получить userId из токена: {}", e.getMessage());
            throw new JwtException("Невалидный токен");
        }
    }

    public Set<String> getRoles(String token) {
        try {
            String rolesString = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("roles", String.class);

            if (rolesString == null || rolesString.trim().isEmpty()) {
                return Set.of();
            }

            return Arrays.stream(rolesString.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
        } catch (JwtException e) {
            log.warn("Не удалось получить roles из токена: {}", e.getMessage());
            return Set.of();
        }
    }

    public Set<GrantedAuthority> getAuthorities(String token) {
        Set<String> roles = getRoles(token);

        return roles.stream()
                .filter(role -> !role.trim().isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    public Claims getAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("Не удалось получить claims из токена: {}", e.getMessage());
            throw new JwtException("Невалидный токен");
        }
    }

    public Date getIssuedAt(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getIssuedAt();
        } catch (JwtException e) {
            log.warn("Не удалось получить issuedAt из токена: {}", e.getMessage());
            return null;
        }
    }

    public Date getExpiration(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
        } catch (JwtException e) {
            log.warn("Не удалось получить expiration из токена: {}", e.getMessage());
            return null;
        }
    }
}
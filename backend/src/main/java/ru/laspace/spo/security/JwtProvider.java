package ru.laspace.spo.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.config.JwtProperties;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Authentication authentication, Long userId, Set<String> roles) {
        String authorities = roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(authentication.getName())
                .claim("userId", userId)
                .claim("roles", authorities)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtProperties.getIssuer())
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(String username, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtProperties.getIssuer())
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
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

            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT токен истек");
            return false;
        } catch (JwtException e) {
            log.debug("Невалидный JWT токен: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = getUserIdFromToken(token);

        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("Пользователь не найден: {}", userId);
            return new UsernameNotFoundException("Пользователь не найден");
        });

        if (!user.isEnabled()) {
            log.warn("Аккаунт отключен, ID: {}", userId);
            throw new UsernameNotFoundException("Аккаунт отключен.");
        }

        String rolesString = claims.get("roles", String.class);
        if (rolesString == null || rolesString.trim().isEmpty()) {
            log.warn("Токен не содержит ролей");
            throw new JwtException("Токен не содержит ролей");
        }

        Collection<? extends GrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                .filter(auth -> !auth.trim().isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        return new UsernamePasswordAuthenticationToken(userDetails, token, authorities);
    }

}

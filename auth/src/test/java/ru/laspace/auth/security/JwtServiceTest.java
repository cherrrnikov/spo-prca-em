package ru.laspace.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ru.laspace.auth.config.JwtProperties;
import ru.laspace.auth.exception.AuthException;

public class JwtServiceTest {
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-32-chars!!");
        props.setIssuer("test-issuer");
        props.setAccessTokenExpiration(900_000L);
        props.setRefreshTokenExpiration(604_800_000L);
        jwtService = new JwtService(props);
    }

    @Test
    void generateAccessToken_returnsValidToken() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        String token = jwtService.generateAccessToken("igor", 1L, authorities);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void generateAccessToken_usernameAndIdExtractedCorrectly() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        String token = jwtService.generateAccessToken("igor", 42L, authorities);

        assertThat(jwtService.getUsername(token)).isEqualTo("igor");
        assertThat(jwtService.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void generateAccessToken_rolesExtractedCorrectly() {
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER"));

        String token = jwtService.generateAccessToken("igor", 42L, authorities);

        Set<? extends GrantedAuthority> result = jwtService.getAuthorities(token);

        assertThat(result)
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("test-secret-key-must-be-at-least-32-chars!!");
        shortProps.setIssuer("test-issuer");
        shortProps.setAccessTokenExpiration(-1000L);
        shortProps.setRefreshTokenExpiration(604_800_000L);

        JwtService shortJwt = new JwtService(shortProps);

        String token = shortJwt.generateAccessToken("igor", 1L, List.of());

        assertThat(shortJwt.validateToken(token)).isFalse();
    }

    @Test
    void validateTokenType_accessToken_correctType() {
        String token = jwtService.generateAccessToken("igor", 1L, List.of());

        assertThat(jwtService.validateTokenType(token, "access")).isTrue();
        assertThat(jwtService.validateTokenType(token, "refresh")).isFalse();
    }

    @Test
    void generateRefreshToken_returnsValidToken() {
        String token = jwtService.generateRefreshToken("igor", 1L);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void generateRefreshToken_correctType() {
        String token = jwtService.generateRefreshToken("igor", 1L);

        assertThat(jwtService.validateTokenType(token, "refresh")).isTrue();
        assertThat(jwtService.validateTokenType(token, "access")).isFalse();
    }

    @Test
    void validateToken_invalidString_returnsFalse() {
        assertThat(jwtService.validateToken("not-a-token")).isFalse();
    }

    @Test
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtService.validateToken("")).isFalse();
    }

    @Test
    void getUsername_invalidToken_throwsAuthException() {
        assertThatThrownBy(() -> jwtService.getUsername("invalid-token-here"))
                .isInstanceOf(AuthException.class);
    }
}

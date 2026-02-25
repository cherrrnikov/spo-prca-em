package ru.laspace.auth.service.impl;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.config.SecurityProperties;
import ru.laspace.auth.dto.request.LoginRequest;
import ru.laspace.auth.dto.request.RefreshTokenRequest;
import ru.laspace.auth.dto.response.JwtResponse;
import ru.laspace.auth.dto.response.UserResponse;
import ru.laspace.auth.entity.RefreshToken;
import ru.laspace.auth.entity.Role;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.exception.AuthException;
import ru.laspace.auth.repository.RoleRepository;
import ru.laspace.auth.repository.UserRepository;
import ru.laspace.auth.security.CustomUserDetails;
import ru.laspace.auth.security.JwtService;
import ru.laspace.auth.service.AuthService;
import ru.laspace.auth.service.LoginAttemptService;
import ru.laspace.auth.service.RefreshTokenService;
import ru.laspace.auth.service.UserCacheService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;
    private final UserCacheService userCacheService;

    @Override
    @Transactional
    public JwtResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getUsername());

        try {
            // Проверка блокировки аккаунта
            if (loginAttemptService.isAccountLocked(request.getUsername())) {
                throw new AuthException("Account is locked. Try again later.");
            }

            // Аутентификация
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            // Сброс счетчика неудачных попыток
            user.resetFailedAttempts();
            user.setLastLoginAt(LocalDateTime.now());

            user = userRepository.save(user);
            userCacheService.updateCachedUser(user);

            // Генерация токенов
            String accessToken = jwtService.generateAccessToken(
                    user.getUsername(), user.getId(), userDetails.getAuthorities());

            String refreshToken = refreshTokenService.createRefreshToken(
                    user,
                    getClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent"));

            log.info("User logged in successfully: {}", user.getUsername());

            return buildJwtResponse(accessToken, refreshToken, user);

        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(request.getUsername());
            throw new AuthException("Invalid username or password");
        }
    }

    @Override
    @Transactional
    public JwtResponse refresh(RefreshTokenRequest request) {
        log.info("Refresh token request");

        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        if (!user.isEnabled()) {
            throw new AuthException("Account is disabled");
        }

        if (user.isAccountLocked() && !securityProperties.isLockExpired(user.getLockTime())) {
            throw new AuthException("Account is locked");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtService.generateAccessToken(
                user.getUsername(), user.getId(), userDetails.getAuthorities());

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildJwtResponse(newAccessToken, refreshToken.getToken(), user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        log.info("Logout request");

        try {
            RefreshToken token = refreshTokenService.verifyRefreshToken(refreshToken);
            User user = token.getUser();

            user.setLastLogoutAt(LocalDateTime.now());
            userRepository.save(user);

            refreshTokenService.revokeRefreshToken(refreshToken);
            log.info("User logged out: {}", user.getUsername());
        } catch (Exception e) {
            log.warn("Logout with invalid token: {}", e.getMessage());
        }

        SecurityContextHolder.clearContext();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private JwtResponse buildJwtResponse(String accessToken, String refreshToken, User user) {
        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }

    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .accountLocked(user.isAccountLocked())
                .failedAttempts(user.getFailedAttempts())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }
}
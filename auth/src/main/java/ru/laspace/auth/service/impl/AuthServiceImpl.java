package ru.laspace.auth.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.config.SecurityProperties;
import ru.laspace.auth.dto.cache.UserCacheDto;
import ru.laspace.auth.dto.request.LoginRequest;
import ru.laspace.auth.dto.request.RefreshTokenRequest;
import ru.laspace.auth.dto.response.JwtResponse;
import ru.laspace.auth.entity.RefreshToken;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.exception.AuthException;
import ru.laspace.auth.exception.NotFoundException;
import ru.laspace.auth.exception.TokenRefreshException;
import ru.laspace.auth.mapper.UserMapper;
import ru.laspace.auth.repository.RefreshTokenRepository;
import ru.laspace.auth.repository.UserRepository;
import ru.laspace.auth.security.JwtAuthenticationProvider;
import ru.laspace.auth.security.JwtGenerator;
import ru.laspace.auth.security.JwtParser;
import ru.laspace.auth.security.JwtValidator;
import ru.laspace.auth.security.UserDetailsImpl;
import ru.laspace.auth.security.UserDetailsServiceImpl;
import ru.laspace.auth.service.AuthService;
import ru.laspace.auth.service.LoginAttemptService;
import ru.laspace.auth.service.RefreshTokenService;
import ru.laspace.auth.service.UserCacheService;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtGenerator jwtGenerator;
    private final JwtValidator jwtValidator;
    private final JwtParser jwtParser;
    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityProperties securityProperties;
    private final UserCacheService userCacheService;
    private final UserDetailsServiceImpl userDetailsService;

    @SuppressWarnings("null")
    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        log.info("Попытка входа: {}", loginRequest.getUsername());

        if (securityProperties.isEnableBruteForceProtection() &&
                loginAttemptService.isAccountLocked(loginRequest.getUsername())) {
            int remainingAttempts = loginAttemptService.getRemainingAttempts(loginRequest.getUsername());
            log.warn("Попытка входа в заблокированный аккаунт: {}. Осталось попыток: {}",
                    loginRequest.getUsername(), remainingAttempts);
            throw new AuthException(
                    "Аккаунт заблокирован из-за слишком большого количества неудачных попыток. Попробуйте позже.");
        }

        try {
            log.debug("Пытаемся аутентифицировать через AuthenticationManager...");
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            log.debug("Аутентификация успешна!");

            loginAttemptService.loginSucceeded(loginRequest.getUsername());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User user = userDetails.getUser();

            String accessToken = jwtGenerator.generateAccessToken(authentication);

            String refreshTokenValue = null;

            Optional<RefreshToken> existingToken = refreshTokenRepository
                    .findAllValidTokensByUser(user.getId())
                    .stream()
                    .filter(token -> token.getExpiryDate().isAfter(LocalDateTime.now()))
                    .findFirst();

            if (existingToken.isPresent()) {
                refreshTokenValue = existingToken.get().getToken();
                log.debug("Используем существующий refresh token для пользователя ID={}", user.getId());
            } else {
                refreshTokenValue = jwtGenerator.generateRefreshToken(user.getUsername(), user.getId());
                RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user, refreshTokenValue);
                refreshTokenRepository.save(refreshTokenEntity);
                log.debug("Создан новый refresh token для пользователя ID={}", user.getId());
            }

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            UserCacheDto userCacheDto = userCacheService.convertToCacheDTO(userDetails);
            userCacheService.cacheUser(user.getUsername(), userCacheDto);

            return userMapper.toJwtResponse(accessToken, refreshTokenValue, user);
        } catch (BadCredentialsException e) {
            log.warn("Неверный логин или пароль: {}", loginRequest.getUsername());

            loginAttemptService.loginFailed(loginRequest.getUsername());

            int remainingAttempts = loginAttemptService.getRemainingAttempts(loginRequest.getUsername());

            throw new AuthException(
                    String.format("Неверный логин или пароль. Осталось попыток: %d", remainingAttempts));
        } catch (AuthenticationException e) {
            log.error("Ошибка входа {}: {}", loginRequest.getUsername(), e.getMessage());

            loginAttemptService.loginFailed(loginRequest.getUsername());

            throw new AuthException("Аутентификация провалена: " + e.getMessage());
        }
    }

    @SuppressWarnings("null")
    @Override
    public JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("Запрос на refreshToken");
        String refreshTokenValue = refreshTokenRequest.getRefreshToken();

        if (!jwtValidator.validateToken(refreshTokenValue)) {
            throw new TokenRefreshException("Неверный refreshToken");
        }

        Long userId = jwtParser.getUserId(refreshTokenValue);

        RefreshToken storedToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);

        UserCacheDto cachedUser = userCacheService.getCachedUserById(userId);
        UserDetailsImpl userDetails;
        if (cachedUser != null) {
            userDetails = userCacheService.createUserDetailsFromCache(cachedUser);
        } else {
            userDetails = (UserDetailsImpl) userDetailsService.loadUserById(userId);
            UserCacheDto newCacheDto = userCacheService.convertToCacheDTO(userDetails);
            userCacheService.cacheUser(userDetails.getUsername(), newCacheDto);
        }

        if (userDetails == null) {
            throw new NotFoundException("Пользователь не найден");
        }

        User user = userDetails.getUser();

        if (!user.isEnabled()) {
            throw new TokenRefreshException("Аккаунт отключен");
        }

        if (!userDetails.isAccountNonLocked()) {
            throw new TokenRefreshException("Аккаунт заблокирован");
        }

        Authentication authentication = jwtAuthenticationProvider.getAuthenticationFromUserDetails(
                userDetails,
                refreshTokenValue);

        String newAccessToken = jwtGenerator.generateAccessTokenFromUserDetails(userDetails);
        String sameRefreshToken = storedToken.getToken();

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.debug("Refresh token успешно использован для пользователя ID={}. Выдан новый accessToken", userId);

        return userMapper.toJwtResponse(newAccessToken, sameRefreshToken, user);
    }

    @SuppressWarnings("null")
    @Override
    public void logout(String refreshToken) {
        log.info("Запрос на выход из системы");

        if (!jwtValidator.validateToken(refreshToken)) {
            throw new TokenRefreshException("Неверный refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new TokenRefreshException("Refresh token не найден"));

        if (storedToken.isRevoked()) {
            throw new TokenRefreshException("Refresh token уже отозван");
        }

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new TokenRefreshException("Refresh token истек");
        }

        User user = storedToken.getUser();

        user.setLastLogoutAt(LocalDateTime.now());
        userRepository.save(user);

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        userCacheService.evictUserCache(user.getUsername());

        log.info("Пользователь ID={} вышел из системы. Refresh token отозван", user.getId());
    }
}
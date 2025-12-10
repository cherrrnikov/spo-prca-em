package ru.laspace.spo.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import ru.laspace.spo.dto.request.LoginRequest;
import ru.laspace.spo.dto.request.RefreshTokenRequest;
import ru.laspace.spo.dto.response.JwtResponse;
import ru.laspace.spo.entity.RefreshToken;
import ru.laspace.spo.entity.Role;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.exception.AuthException;
import ru.laspace.spo.exception.NotFoundException;
import ru.laspace.spo.exception.TokenRefreshException;
import ru.laspace.spo.mapper.UserMapper;
import ru.laspace.spo.repository.RefreshTokenRepository;
import ru.laspace.spo.repository.UserRepository;
import ru.laspace.spo.security.JwtProvider;
import ru.laspace.spo.security.UserDetailsImpl;
import ru.laspace.spo.service.AuthService;
import ru.laspace.spo.service.RefreshTokenService;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;

    @SuppressWarnings("null")
    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        log.info("Попытка входа: {}", loginRequest.getUsername());
        try {
            log.debug("Пытаемся аутентифицировать через AuthenticationManager...");
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            log.debug("Аутентификация успешна!");
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User user = userDetails.getUser();

            Set<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());

            String accessToken = jwtProvider.generateAccessToken(authentication, user.getId(), roles);

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
                refreshTokenValue = jwtProvider.generateRefreshToken(user.getUsername(), user.getId());
                RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user, refreshTokenValue);
                refreshTokenRepository.save(refreshTokenEntity);
                log.debug("Создан новый refresh token для пользователя ID={}", user.getId());
            }

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            return userMapper.toJwtResponse(accessToken, refreshTokenValue, user);
        } catch (BadCredentialsException e) {
            log.warn("Неверный логин или пароль: {}", loginRequest.getUsername());
            throw new AuthException("Неверный логин или пароль");
        } catch (AuthenticationException e) {
            log.error("Ошибка входа {}: {}", loginRequest.getUsername(), e.getMessage());
            throw new AuthException("Аутентификация провалена");
        }
    }

    @SuppressWarnings("null")
    @Override
    public JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("Запрос на refreshToken");
        String refreshTokenValue = refreshTokenRequest.getRefreshToken();

        if (!jwtProvider.validateToken(refreshTokenValue)) {
            throw new TokenRefreshException("Неверный refreshToken");
        }

        Long userId = jwtProvider.getUserIdFromToken(refreshTokenValue);

        RefreshToken storedToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (!user.isEnabled()) {
            throw new TokenRefreshException("Аккаунт отключен");
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());

        String newAccessToken = jwtProvider.generateAccessToken(authentication, user.getId(), roles);
        String sameRefreshToken = storedToken.getToken();

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.debug("Refresh token успешно использован для пользователя ID={}. Выдан новый access token", userId);

        return userMapper.toJwtResponse(newAccessToken, sameRefreshToken, user);
    }

    @SuppressWarnings("null")
    @Override
    public void logout(String refreshToken) {
        log.info("Запрос на выход из системы");

        if (!jwtProvider.validateToken(refreshToken)) {
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

        log.info("Пользователь ID={} вышел из системы. Refresh token отозван", user.getId());
    }
}

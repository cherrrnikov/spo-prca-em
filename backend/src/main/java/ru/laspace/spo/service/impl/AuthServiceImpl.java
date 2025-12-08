package ru.laspace.spo.service.impl;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
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
import ru.laspace.spo.repository.RefreshTokenRepository;
import ru.laspace.spo.repository.UserRepository;
import ru.laspace.spo.security.JwtProvider;
import ru.laspace.spo.security.UserDetailsImpl;
import ru.laspace.spo.service.AuthService;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Override
    public User authenticate(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getUser();

        } catch (BadCredentialsException e) {
            throw new AuthException("Неверный логин или пароль");
        } catch (AuthenticationException e) {
            throw new AuthException("Аутентификация провалена");
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @SuppressWarnings("null")
    @Override
    public User findById(Long userId) {
        log.debug("Поиск пользователя по ID: {}", userId);

        return userRepository.findById(userId).orElseThrow(() -> {
            log.warn("Пользователь не найден по ID: {}", userId);
            return new NotFoundException("Пользователь не найден");
        });
    }

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
            String refreshToken = jwtProvider.generateRefreshToken(user.getUsername(), user.getId());

            RefreshToken refreshTokenEntity = new RefreshToken();

            refreshTokenEntity.setUser(user);
            refreshTokenEntity.setToken(refreshToken);
            refreshTokenEntity.setExpiryDate(LocalDateTime.now().plusDays(7));
            refreshTokenEntity.setCreatedAt(LocalDateTime.now());
            refreshTokenEntity.setRevoked(false);

            refreshTokenRepository.save(refreshTokenEntity);

            user.setLastLoginAt(LocalDateTime.now());

            userRepository.save(user);

            return JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .roles(roles)
                    .lastLoginAt(user.getLastLoginAt())
                    .build();
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
        String refreshToken = refreshTokenRequest.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new TokenRefreshException("Неверный refreshToken");
        }

        String username = jwtProvider.getUsernameFromToken(refreshToken);
        Long userId = jwtProvider.getUserIdFromToken(refreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new TokenRefreshException("RefreshToken не найден"));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new TokenRefreshException("RefreshToken истёк");
        }

        if (storedToken.isRevoked()) {
            throw new TokenRefreshException("RefreshToken аннулирован");
        }

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
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getUsername(), user.getId());

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        RefreshToken newRefreshTokenEntity = new RefreshToken();

        newRefreshTokenEntity.setUser(user);
        newRefreshTokenEntity.setToken(newRefreshToken);
        newRefreshTokenEntity.setExpiryDate(LocalDateTime.now().plusDays(7));
        newRefreshTokenEntity.setCreatedAt(LocalDateTime.now());
        newRefreshTokenEntity.setRevoked(false);

        refreshTokenRepository.save(newRefreshTokenEntity);

        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles)
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    @SuppressWarnings("null")
    @Override
    public void logout(String refreshToken, Long userId) {
        log.info("Выход из системы: {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        user.setLastLogoutAt(LocalDateTime.now());

        userRepository.save(user);

        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    if (token.getUser().getId().equals(userId)) {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    } else {
                        log.warn("Попытка отозвать чужой refreshToken для пользователя {}", userId);
                    }
                });
    }
}

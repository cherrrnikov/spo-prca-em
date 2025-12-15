package ru.laspace.spo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ru.laspace.spo.config.SecurityProperties;
import ru.laspace.spo.dto.cache.UserCacheDto;
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
import ru.laspace.spo.security.JwtAuthenticationProvider;
import ru.laspace.spo.security.JwtGenerator;
import ru.laspace.spo.security.JwtParser;
import ru.laspace.spo.security.JwtValidator;
import ru.laspace.spo.security.UserDetailsImpl;
import ru.laspace.spo.security.UserDetailsServiceImpl;
import ru.laspace.spo.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для AuthServiceImpl")
class AuthServiceImplTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private RefreshTokenRepository refreshTokenRepository;

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private JwtGenerator jwtGenerator;

        @Mock
        private JwtValidator jwtValidator;

        @Mock
        private JwtParser jwtParser;

        @Mock
        private JwtAuthenticationProvider jwtAuthenticationProvider;

        @Mock
        private UserMapper userMapper;

        @Mock
        private RefreshTokenService refreshTokenService;

        @Mock
        private LoginAttemptService loginAttemptService;

        @Mock
        private SecurityProperties securityProperties;

        @Mock
        private UserCacheService userCacheService;

        @Mock
        private UserDetailsServiceImpl userDetailsService;

        @InjectMocks
        private AuthServiceImpl authService;

        private User testUser;
        private Role userRole;
        private Role adminRole;
        private Set<Role> roles;
        private UserDetailsImpl userDetails;
        private Authentication authentication;
        private RefreshToken refreshToken;
        private UserCacheDto userCacheDto;

        @BeforeEach
        void setUp() {
                SecurityContextHolder.clearContext();

                userRole = new Role();
                userRole.setId(1L);
                userRole.setName("USER");

                adminRole = new Role();
                adminRole.setId(2L);
                adminRole.setName("ADMIN");

                roles = new HashSet<>();
                roles.add(userRole);
                roles.add(adminRole);

                testUser = new User();
                testUser.setId(1L);
                testUser.setUsername("testuser");
                testUser.setPasswordHash("hashedPassword");
                testUser.setFirstName("Test");
                testUser.setLastName("User");
                testUser.setEnabled(true);
                testUser.setAccountLocked(false);
                testUser.setFailedAttempts(0);
                testUser.setRoles(roles);

                userDetails = new UserDetailsImpl(testUser, securityProperties);
                authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                "credentials",
                                userDetails.getAuthorities());

                refreshToken = new RefreshToken();
                refreshToken.setId(1L);
                refreshToken.setToken("refreshToken123");
                refreshToken.setUser(testUser);
                refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
                refreshToken.setRevoked(false);
                refreshToken.setCreatedAt(LocalDateTime.now());

                userCacheDto = new UserCacheDto();
                userCacheDto.setId(1L);
                userCacheDto.setUsername("testuser");
                userCacheDto.setFirstName("Test");
                userCacheDto.setLastName("User");
                userCacheDto.setEnabled(true);
                userCacheDto.setAccountLocked(false);
                userCacheDto.setFailedAttempts(0);
                userCacheDto.setRoles(Set.of("USER", "ADMIN"));
        }

        @Test
        @DisplayName("login - успешный вход с существующим refresh token")
        void login_WhenValidCredentialsAndExistingToken_ReturnsJwtResponseWithExistingToken() {
                LoginRequest request = new LoginRequest("testuser", "password123");

                when(securityProperties.isEnableBruteForceProtection()).thenReturn(true);
                when(loginAttemptService.isAccountLocked("testuser")).thenReturn(false);
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);
                when(jwtGenerator.generateAccessToken(authentication)).thenReturn("accessToken123");

                RefreshToken existingToken = new RefreshToken();
                existingToken.setToken("existingRefreshToken");
                existingToken.setExpiryDate(LocalDateTime.now().plusDays(1));

                when(refreshTokenRepository.findAllValidTokensByUser(1L))
                                .thenReturn(List.of(existingToken));

                when(userRepository.save(any(User.class))).thenReturn(testUser);
                when(userCacheService.convertToCacheDTO(userDetails)).thenReturn(userCacheDto);
                when(userCacheService.cacheUser("testuser", userCacheDto)).thenReturn(userCacheDto);

                JwtResponse expectedResponse = JwtResponse.builder()
                                .accessToken("accessToken123")
                                .refreshToken("existingRefreshToken")
                                .username("testuser")
                                .firstName("Test")
                                .lastName("User")
                                .roles(Set.of("USER", "ADMIN"))
                                .lastLoginAt(testUser.getLastLoginAt())
                                .build();

                when(userMapper.toJwtResponse("accessToken123", "existingRefreshToken", testUser))
                                .thenReturn(expectedResponse);

                JwtResponse response = authService.login(request);

                assertThat(response).isNotNull();
                assertThat(response.getAccessToken()).isEqualTo("accessToken123");
                assertThat(response.getRefreshToken()).isEqualTo("existingRefreshToken");

                verify(loginAttemptService).loginSucceeded("testuser");
                verify(loginAttemptService, never()).loginFailed(anyString());
                verify(refreshTokenRepository, never()).save(any(RefreshToken.class)); // Не сохраняем новый
                verify(userRepository).save(testUser);
                verify(userCacheService).cacheUser("testuser", userCacheDto);
        }

        @Test
        @DisplayName("login - успешный вход с созданием нового refresh token")
        void login_WhenValidCredentialsAndNoExistingToken_ReturnsJwtResponseWithNewToken() {
                LoginRequest request = new LoginRequest("testuser", "password123");

                when(securityProperties.isEnableBruteForceProtection()).thenReturn(true);
                when(loginAttemptService.isAccountLocked("testuser")).thenReturn(false);
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);
                when(jwtGenerator.generateAccessToken(authentication)).thenReturn("accessToken123");
                when(jwtGenerator.generateRefreshToken("testuser", 1L)).thenReturn("newRefreshToken123");

                when(refreshTokenService.createRefreshToken(testUser, "newRefreshToken123"))
                                .thenReturn(refreshToken);
                when(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken);
                when(userRepository.save(any(User.class))).thenReturn(testUser);
                when(userCacheService.convertToCacheDTO(userDetails)).thenReturn(userCacheDto);
                when(userCacheService.cacheUser("testuser", userCacheDto)).thenReturn(userCacheDto);

                JwtResponse expectedResponse = JwtResponse.builder()
                                .accessToken("accessToken123")
                                .refreshToken("newRefreshToken123")
                                .username("testuser")
                                .firstName("Test")
                                .lastName("User")
                                .roles(Set.of("USER", "ADMIN"))
                                .build();
                when(userMapper.toJwtResponse("accessToken123", "newRefreshToken123", testUser))
                                .thenReturn(expectedResponse);

                JwtResponse response = authService.login(request);

                assertThat(response).isNotNull();
                assertThat(response.getAccessToken()).isEqualTo("accessToken123");
                assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken123");

                verify(refreshTokenService).createRefreshToken(testUser, "newRefreshToken123");
                verify(refreshTokenRepository).save(refreshToken);
                verify(userRepository).save(testUser);
                assertThat(testUser.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("login - аккаунт заблокирован (brute force protection включена)")
        void login_WhenAccountLockedAndBruteForceEnabled_ThrowsAuthException() {
                LoginRequest request = new LoginRequest("testuser", "password123");

                when(securityProperties.isEnableBruteForceProtection()).thenReturn(true);
                when(loginAttemptService.isAccountLocked("testuser")).thenReturn(true);
                when(loginAttemptService.getRemainingAttempts("testuser")).thenReturn(0);

                assertThatThrownBy(() -> authService.login(request))
                                .isInstanceOf(AuthException.class)
                                .hasMessageContaining("Аккаунт заблокирован");

                verify(authenticationManager, never()).authenticate(any());
                verify(loginAttemptService, never()).loginSucceeded(anyString());
                verify(loginAttemptService, never()).loginFailed(anyString());
        }

        @Test
        @DisplayName("login - неверные учетные данные (BadCredentialsException)")
        void login_WhenInvalidCredentials_ThrowsAuthExceptionWithRemainingAttempts() {
                LoginRequest request = new LoginRequest("testuser", "wrongpassword");

                when(securityProperties.isEnableBruteForceProtection()).thenReturn(true);
                when(loginAttemptService.isAccountLocked("testuser")).thenReturn(false);
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Bad credentials"));
                when(loginAttemptService.getRemainingAttempts("testuser")).thenReturn(4);

                assertThatThrownBy(() -> authService.login(request))
                                .isInstanceOf(AuthException.class)
                                .hasMessageContaining("Неверный логин или пароль")
                                .hasMessageContaining("Осталось попыток: 4");

                verify(loginAttemptService).loginFailed("testuser");
                verify(loginAttemptService, never()).loginSucceeded(anyString());
        }

        @Test
        @DisplayName("login - другие AuthenticationException (например, LockedException)")
        void login_WhenOtherAuthenticationException_ThrowsAuthException() {
                LoginRequest request = new LoginRequest("testuser", "password123");

                when(securityProperties.isEnableBruteForceProtection()).thenReturn(true);
                when(loginAttemptService.isAccountLocked("testuser")).thenReturn(false);
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new LockedException("Account is locked"));

                assertThatThrownBy(() -> authService.login(request))
                                .isInstanceOf(AuthException.class)
                                .hasMessageContaining("Аутентификация провалена");

                verify(loginAttemptService).loginFailed("testuser");
        }

        @Test
        @DisplayName("login - brute force protection отключена")
        void login_WhenBruteForceProtectionDisabled_DoesNotCheckLock() {
                LoginRequest request = new LoginRequest("testuser", "password123");

                when(securityProperties.isEnableBruteForceProtection()).thenReturn(false);
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);
                when(jwtGenerator.generateAccessToken(authentication)).thenReturn("accessToken123");
                when(jwtGenerator.generateRefreshToken("testuser", 1L)).thenReturn("newRefreshToken123");
                when(refreshTokenService.createRefreshToken(testUser, "newRefreshToken123"))
                                .thenReturn(refreshToken);
                when(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken);
                when(userRepository.save(any(User.class))).thenReturn(testUser);
                when(userCacheService.convertToCacheDTO(userDetails)).thenReturn(userCacheDto);
                when(userCacheService.cacheUser("testuser", userCacheDto)).thenReturn(userCacheDto);

                JwtResponse expectedResponse = JwtResponse.builder()
                                .accessToken("accessToken123")
                                .refreshToken("newRefreshToken123")
                                .username("testuser")
                                .build();
                when(userMapper.toJwtResponse("accessToken123", "newRefreshToken123", testUser))
                                .thenReturn(expectedResponse);

                JwtResponse response = authService.login(request);

                assertThat(response).isNotNull();
                verify(loginAttemptService, never()).isAccountLocked(anyString());
                verify(loginAttemptService).loginSucceeded("testuser");
        }

        @Test
        @DisplayName("refreshToken - успешное обновление с кэшированным пользователем")
        void refreshToken_WhenValidTokenAndCachedUser_ReturnsNewTokens() {
                RefreshTokenRequest request = new RefreshTokenRequest("refreshToken123");

                when(jwtValidator.validateToken("refreshToken123")).thenReturn(true);
                when(jwtParser.getUserId("refreshToken123")).thenReturn(1L);
                when(refreshTokenService.verifyRefreshToken("refreshToken123")).thenReturn(refreshToken);
                when(userCacheService.getCachedUserById(1L)).thenReturn(userCacheDto);
                when(userCacheService.createUserDetailsFromCache(userCacheDto)).thenReturn(userDetails);
                when(jwtAuthenticationProvider.getAuthenticationFromUserDetails(userDetails, "refreshToken123"))
                                .thenReturn(authentication);
                when(jwtGenerator.generateAccessTokenFromUserDetails(userDetails)).thenReturn("newAccessToken123");
                when(userRepository.save(any(User.class))).thenReturn(testUser);

                JwtResponse expectedResponse = JwtResponse.builder()
                                .accessToken("newAccessToken123")
                                .refreshToken("refreshToken123")
                                .username("testuser")
                                .build();
                when(userMapper.toJwtResponse("newAccessToken123", "refreshToken123", testUser))
                                .thenReturn(expectedResponse);

                JwtResponse response = authService.refreshToken(request);

                assertThat(response).isNotNull();
                assertThat(response.getAccessToken()).isEqualTo("newAccessToken123");
                assertThat(response.getRefreshToken()).isEqualTo("refreshToken123");

                verify(userCacheService, never()).cacheUser(anyString(), any());
                verify(userRepository).save(testUser);
                assertThat(testUser.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("refreshToken - успешное обновление без кэшированного пользователя")
        void refreshToken_WhenValidTokenAndNoCachedUser_LoadsFromDbAndCaches() {
                RefreshTokenRequest request = new RefreshTokenRequest("refreshToken123");

                when(jwtValidator.validateToken("refreshToken123")).thenReturn(true);
                when(jwtParser.getUserId("refreshToken123")).thenReturn(1L);
                when(refreshTokenService.verifyRefreshToken("refreshToken123")).thenReturn(refreshToken);

                when(userCacheService.getCachedUserById(1L)).thenReturn(null);
                when(userDetailsService.loadUserById(1L)).thenReturn(userDetails);
                when(userCacheService.convertToCacheDTO(userDetails)).thenReturn(userCacheDto);

                when(jwtAuthenticationProvider.getAuthenticationFromUserDetails(userDetails, "refreshToken123"))
                                .thenReturn(authentication);
                when(jwtGenerator.generateAccessTokenFromUserDetails(userDetails)).thenReturn("newAccessToken123");
                when(userRepository.save(any(User.class))).thenReturn(testUser);

                JwtResponse expectedResponse = JwtResponse.builder()
                                .accessToken("newAccessToken123")
                                .refreshToken("refreshToken123")
                                .username("testuser")
                                .build();
                when(userMapper.toJwtResponse("newAccessToken123", "refreshToken123", testUser))
                                .thenReturn(expectedResponse);

                JwtResponse response = authService.refreshToken(request);

                assertThat(response).isNotNull();
                verify(userCacheService).cacheUser("testuser", userCacheDto);
                verify(userDetailsService).loadUserById(1L);
        }

        @Test
        @DisplayName("refreshToken - невалидный refresh token")
        void refreshToken_WhenInvalidToken_ThrowsTokenRefreshException() {
                RefreshTokenRequest request = new RefreshTokenRequest("invalidToken");

                when(jwtValidator.validateToken("invalidToken")).thenReturn(false);

                assertThatThrownBy(() -> authService.refreshToken(request))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("Неверный refreshToken");
        }

        @Test
        @DisplayName("refreshToken - отключенный аккаунт")
        void refreshToken_WhenUserDisabled_ThrowsTokenRefreshException() {
                RefreshTokenRequest request = new RefreshTokenRequest("refreshToken123");
                testUser.setEnabled(false);

                when(jwtValidator.validateToken("refreshToken123")).thenReturn(true);
                when(jwtParser.getUserId("refreshToken123")).thenReturn(1L);
                when(refreshTokenService.verifyRefreshToken("refreshToken123")).thenReturn(refreshToken);
                when(userCacheService.getCachedUserById(1L)).thenReturn(userCacheDto);
                when(userCacheService.createUserDetailsFromCache(userCacheDto)).thenReturn(userDetails);

                assertThatThrownBy(() -> authService.refreshToken(request))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("Аккаунт отключен");

                testUser.setEnabled(true);
        }

        @Test
        @DisplayName("refreshToken - заблокированный аккаунт")
        void refreshToken_WhenUserLocked_ThrowsTokenRefreshException() {
                RefreshTokenRequest request = new RefreshTokenRequest("refreshToken123");
                testUser.setAccountLocked(true);
                testUser.setLockTime(LocalDateTime.now().minusMinutes(5));

                when(jwtValidator.validateToken("refreshToken123")).thenReturn(true);
                when(jwtParser.getUserId("refreshToken123")).thenReturn(1L);
                when(refreshTokenService.verifyRefreshToken("refreshToken123")).thenReturn(refreshToken);
                when(userCacheService.getCachedUserById(1L)).thenReturn(userCacheDto);
                when(userCacheService.createUserDetailsFromCache(userCacheDto)).thenReturn(userDetails);

                assertThatThrownBy(() -> authService.refreshToken(request))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("Аккаунт заблокирован");

                testUser.setAccountLocked(false);
                testUser.setLockTime(null);
        }

        @Test
        @DisplayName("refreshToken - пользователь не найден")
        void refreshToken_WhenUserNotFound_ThrowsNotFoundException() {
                RefreshTokenRequest request = new RefreshTokenRequest("refreshToken123");
                when(jwtValidator.validateToken("refreshToken123")).thenReturn(true);
                when(jwtParser.getUserId("refreshToken123")).thenReturn(1L);
                when(refreshTokenService.verifyRefreshToken("refreshToken123")).thenReturn(refreshToken);
                when(userCacheService.getCachedUserById(1L)).thenReturn(null);
                when(userDetailsService.loadUserById(1L)).thenThrow(new NotFoundException("Пользователь не найден"));

                assertThatThrownBy(() -> authService.refreshToken(request))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessage("Пользователь не найден");
        }

        @Test
        @DisplayName("logout - успешный выход из системы")
        void logout_WhenValidToken_RevokesTokenAndUpdatesUser() {
                String token = "refreshToken123";

                when(jwtValidator.validateToken(token)).thenReturn(true);
                when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));
                when(userRepository.save(any(User.class))).thenReturn(testUser);
                when(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken);

                authService.logout(token);

                verify(userRepository).save(testUser);
                verify(refreshTokenRepository).save(refreshToken);
                verify(userCacheService).evictUserCache("testuser");

                assertThat(refreshToken.isRevoked()).isTrue();
                assertThat(testUser.getLastLogoutAt()).isNotNull();
        }

        @Test
        @DisplayName("logout - невалидный refresh token")
        void logout_WhenInvalidToken_ThrowsTokenRefreshException() {
                String token = "invalidToken";

                when(jwtValidator.validateToken(token)).thenReturn(false);

                assertThatThrownBy(() -> authService.logout(token))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("Неверный refresh token");
        }

        @Test
        @DisplayName("logout - refresh token не найден")
        void logout_WhenTokenNotFound_ThrowsTokenRefreshException() {
                String token = "nonexistentToken";

                when(jwtValidator.validateToken(token)).thenReturn(true);
                when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> authService.logout(token))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("Refresh token не найден");
        }

        @Test
        @DisplayName("logout - refresh token уже отозван")
        void logout_WhenTokenAlreadyRevoked_ThrowsTokenRefreshException() {
                String token = "revokedToken";
                refreshToken.setRevoked(true);

                when(jwtValidator.validateToken(token)).thenReturn(true);
                when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

                assertThatThrownBy(() -> authService.logout(token))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("Refresh token уже отозван");

                refreshToken.setRevoked(false);
        }

        @Test
        @DisplayName("logout - истекший refresh token")
        void logout_WhenTokenExpired_ThrowsTokenRefreshException() {
                String token = "expiredToken";
                refreshToken.setExpiryDate(LocalDateTime.now().minusDays(1));

                when(jwtValidator.validateToken(token)).thenReturn(true);
                when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

                assertThatThrownBy(() -> authService.logout(token))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("Refresh token истек");

                refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        }

        @Test
        @DisplayName("logout - исключение при сохранении в БД")
        void logout_WhenSaveFails_ThrowsRuntimeException() {
                String token = "refreshToken123";

                when(jwtValidator.validateToken(token)).thenReturn(true);
                when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));
                when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

                assertThatThrownBy(() -> authService.logout(token))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessage("DB error");

                assertThat(refreshToken.isRevoked()).isFalse();
        }
}
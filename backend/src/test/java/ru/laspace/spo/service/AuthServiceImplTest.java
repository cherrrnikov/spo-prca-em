package ru.laspace.spo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import ru.laspace.spo.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для AuthServiceImpl")
class AuthServiceImplTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private RefreshTokenRepository refreshTokenRepository;

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private JwtProvider jwtProvider;

        @InjectMocks
        private AuthServiceImpl authService;

        private User testUser;
        private Role userRole;
        private Role adminRole;
        private Set<Role> roles;
        private Authentication authentication;
        private UserDetailsImpl userDetails;
        private RefreshToken refreshToken;

        @BeforeEach
        void setUp() {
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
                testUser.setRoles(roles);

                userDetails = new UserDetailsImpl(testUser);
                authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                                userDetails.getAuthorities());

                refreshToken = new RefreshToken();
                refreshToken.setId(1L);
                refreshToken.setToken("refreshToken123");
                refreshToken.setUser(testUser);
                refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
                refreshToken.setRevoked(false);
        }

        @Test
        @DisplayName("authenticate - успешная аутентификация")
        void authenticate_WhenValidCredentials_ReturnsUser() {
                // Arrange
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);

                // Act
                User result = authService.authenticate("testuser", "password123");

                // Assert
                assertThat(result).isEqualTo(testUser);
                verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("authenticate - неверный пароль")
        void authenticate_WhenInvalidPassword_ThrowsAuthException() {
                // Arrange
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                // Act & Assert
                assertThatThrownBy(() -> authService.authenticate("testuser", "wrongpassword"))
                                .isInstanceOf(AuthException.class)
                                .hasMessage("Неверный логин или пароль");
        }

        @Test
        @DisplayName("authenticate - другая ошибка аутентификации")
        void authenticate_WhenOtherAuthenticationError_ThrowsAuthException() {
                // Arrange
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new AuthenticationException("Authentication failed") {
                                });

                // Act & Assert
                // Проверяем только тип исключения, не сообщение
                assertThatThrownBy(() -> authService.authenticate("testuser", "password123"))
                                .isInstanceOf(AuthException.class);

                // Или проверяем, что сообщение содержит "провалена" (без строгой проверки)
                assertThatThrownBy(() -> authService.authenticate("testuser", "password123"))
                                .isInstanceOf(AuthException.class)
                                .hasMessageContaining("провалена");
        }

        @Test
        @DisplayName("existsByUsername - проверка существования пользователя")
        void existsByUsername_WhenCalled_CallsRepository() {
                // Arrange
                when(userRepository.existsByUsername("testuser")).thenReturn(true);

                // Act
                boolean exists = authService.existsByUsername("testuser");

                // Assert
                assertThat(exists).isTrue();
                verify(userRepository).existsByUsername("testuser");
        }

        @Test
        @DisplayName("findById - успешное получение пользователя")
        void findById_WhenUserExists_ReturnsUser() {
                // Arrange
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // Act
                User result = authService.findById(1L);

                // Assert
                assertThat(result).isEqualTo(testUser);
                verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("findById - пользователь не найден")
        void findById_WhenUserNotFound_ThrowsNotFoundException() {
                // Arrange
                when(userRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> authService.findById(999L))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessage("Пользователь не найден");
        }

        @Test
        @DisplayName("login - успешный вход")
        void login_WhenValidCredentials_ReturnsJwtResponse() {
                // Arrange
                LoginRequest request = new LoginRequest("testuser", "password123");

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);
                when(jwtProvider.generateAccessToken(any(Authentication.class), anyLong(), anySet()))
                                .thenReturn("accessToken123");
                when(jwtProvider.generateRefreshToken(anyString(), anyLong()))
                                .thenReturn("refreshToken123");
                when(userRepository.save(any(User.class))).thenReturn(testUser);
                when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

                // Act
                JwtResponse response = authService.login(request);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getAccessToken()).isEqualTo("accessToken123");
                assertThat(response.getRefreshToken()).isEqualTo("refreshToken123");
                assertThat(response.getUsername()).isEqualTo("testuser");
                assertThat(response.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");

                verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
                verify(jwtProvider).generateAccessToken(any(Authentication.class), eq(1L), anySet());
                verify(jwtProvider).generateRefreshToken("testuser", 1L);
                verify(userRepository).save(any(User.class));
                verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("login - неверные учетные данные")
        void login_WhenInvalidCredentials_ThrowsAuthException() {
                // Arrange
                LoginRequest request = new LoginRequest("testuser", "wrongpassword");

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                // Act & Assert
                assertThatThrownBy(() -> authService.login(request))
                                .isInstanceOf(AuthException.class)
                                .hasMessage("Неверный логин или пароль");
        }

        @Test
        @DisplayName("refreshToken - успешное обновление")
        void refreshToken_WhenValidToken_ReturnsNewTokens() {
                // Arrange
                RefreshTokenRequest request = new RefreshTokenRequest("refreshToken123");
                LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

                refreshToken.setExpiryDate(futureDate);

                when(jwtProvider.validateToken("refreshToken123")).thenReturn(true);
                when(jwtProvider.getUsernameFromToken("refreshToken123")).thenReturn("testuser");
                when(jwtProvider.getUserIdFromToken("refreshToken123")).thenReturn(1L);
                when(refreshTokenRepository.findByToken("refreshToken123")).thenReturn(Optional.of(refreshToken));
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(jwtProvider.generateAccessToken(any(Authentication.class), anyLong(), anySet()))
                                .thenReturn("newAccessToken123");
                when(jwtProvider.generateRefreshToken(anyString(), anyLong()))
                                .thenReturn("newRefreshToken123");
                when(userRepository.save(any(User.class))).thenReturn(testUser);
                when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

                // Act
                JwtResponse response = authService.refreshToken(request);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getAccessToken()).isEqualTo("newAccessToken123");
                assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken123");

                verify(jwtProvider).validateToken("refreshToken123");
                verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
                verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("refreshToken - невалидный токен")
        void refreshToken_WhenInvalidToken_ThrowsTokenRefreshException() {
                // Arrange
                RefreshTokenRequest request = new RefreshTokenRequest("invalidToken");

                when(jwtProvider.validateToken("invalidToken")).thenReturn(false);

                // Act & Assert
                assertThatThrownBy(() -> authService.refreshToken(request))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("Неверный refreshToken");
        }

        @Test
        @DisplayName("refreshToken - токен не найден в БД")
        void refreshToken_WhenTokenNotFoundInDb_ThrowsTokenRefreshException() {
                // Arrange
                RefreshTokenRequest request = new RefreshTokenRequest("refreshToken123");

                when(jwtProvider.validateToken("refreshToken123")).thenReturn(true);
                when(jwtProvider.getUsernameFromToken("refreshToken123")).thenReturn("testuser");
                when(jwtProvider.getUserIdFromToken("refreshToken123")).thenReturn(1L);
                when(refreshTokenRepository.findByToken("refreshToken123")).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> authService.refreshToken(request))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("RefreshToken не найден");
        }

        @Test
        @DisplayName("refreshToken - истекший токен")
        void refreshToken_WhenTokenExpired_ThrowsTokenRefreshException() {
                // Arrange
                RefreshTokenRequest request = new RefreshTokenRequest("expiredToken");
                LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

                refreshToken.setExpiryDate(pastDate);

                when(jwtProvider.validateToken("expiredToken")).thenReturn(true);
                when(jwtProvider.getUsernameFromToken("expiredToken")).thenReturn("testuser");
                when(jwtProvider.getUserIdFromToken("expiredToken")).thenReturn(1L);
                when(refreshTokenRepository.findByToken("expiredToken")).thenReturn(Optional.of(refreshToken));

                // Act & Assert
                assertThatThrownBy(() -> authService.refreshToken(request))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("RefreshToken истёк");
        }

        @Test
        @DisplayName("refreshToken - отозванный токен")
        void refreshToken_WhenTokenRevoked_ThrowsTokenRefreshException() {
                // Arrange
                RefreshTokenRequest request = new RefreshTokenRequest("revokedToken");

                refreshToken.setRevoked(true);

                when(jwtProvider.validateToken("revokedToken")).thenReturn(true);
                when(jwtProvider.getUsernameFromToken("revokedToken")).thenReturn("testuser");
                when(jwtProvider.getUserIdFromToken("revokedToken")).thenReturn(1L);
                when(refreshTokenRepository.findByToken("revokedToken")).thenReturn(Optional.of(refreshToken));

                // Act & Assert
                assertThatThrownBy(() -> authService.refreshToken(request))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("RefreshToken аннулирован");
        }

        @Test
        @DisplayName("refreshToken - отключенный аккаунт")
        void refreshToken_WhenUserDisabled_ThrowsTokenRefreshException() {
                // Arrange
                RefreshTokenRequest request = new RefreshTokenRequest("refreshToken123");
                testUser.setEnabled(false);

                when(jwtProvider.validateToken("refreshToken123")).thenReturn(true);
                when(jwtProvider.getUsernameFromToken("refreshToken123")).thenReturn("testuser");
                when(jwtProvider.getUserIdFromToken("refreshToken123")).thenReturn(1L);
                when(refreshTokenRepository.findByToken("refreshToken123")).thenReturn(Optional.of(refreshToken));
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // Act & Assert
                assertThatThrownBy(() -> authService.refreshToken(request))
                                .isInstanceOf(TokenRefreshException.class)
                                .hasMessage("Аккаунт отключен");

                // Cleanup
                testUser.setEnabled(true);
        }

        @Test
        @DisplayName("logout - успешный выход")
        void logout_WhenValidToken_UpdatesUserAndRevokesToken() {
                // Arrange
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(refreshTokenRepository.findByToken("refreshToken123")).thenReturn(Optional.of(refreshToken));
                when(userRepository.save(any(User.class))).thenReturn(testUser);
                when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

                // Act
                authService.logout("refreshToken123", 1L);

                // Assert
                verify(userRepository).save(any(User.class));
                verify(refreshTokenRepository).save(refreshToken);
                assertThat(refreshToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("logout - пользователь не найден")
        void logout_WhenUserNotFound_ThrowsNotFoundException() {
                // Arrange
                when(userRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> authService.logout("refreshToken123", 999L))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessage("Пользователь не найден");
        }

        @Test
        @DisplayName("logout - токен другого пользователя")
        void logout_WhenTokenBelongsToAnotherUser_LogsWarning() {
                // Arrange
                User anotherUser = new User();
                anotherUser.setId(2L);
                anotherUser.setUsername("another");

                // Устанавливаем другого пользователя для токена
                refreshToken.setUser(anotherUser); // ID=2, а не 1!

                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(refreshTokenRepository.findByToken("refreshToken123")).thenReturn(Optional.of(refreshToken));
                when(userRepository.save(any(User.class))).thenReturn(testUser);

                // Act
                authService.logout("refreshToken123", 1L);

                // Assert
                // Проверяем, что токен НЕ сохраняется (userId=1, token.userId=2)
                verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
                verify(userRepository).save(any(User.class));
        }
}
package ru.laspace.spo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ru.laspace.spo.entity.User;
import ru.laspace.spo.exception.AuthException;
import ru.laspace.spo.repository.UserRepository;
import ru.laspace.spo.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @SuppressWarnings("null")
    @Test
    void authenticate_withValidCredentials_returnsUser() {
        String username = "username";
        String firstName = "name";
        String lastName = "surname";
        String password = "password";
        String encodedPassword = "234js$#jsf";

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername(username);
        mockUser.setFirstName(firstName);
        mockUser.setLastName(lastName);
        mockUser.setPasswordHash(encodedPassword);
        mockUser.setEnabled(true);
        mockUser.setLastLoginAt(null);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(mockUser));

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.authenticate(username, password);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(username, result.getUsername());
        assertNotNull(result.getLastLoginAt());
        assertTrue(result.getLastLoginAt().isBefore(LocalDateTime.now().plusSeconds(1)));

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, encodedPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void authenticate_userNotFound_throwsAuthException() {
        String username = "unknown";

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.authenticate(username, "anyPassword"));

        assertEquals("Неверный логин или пароль", exception.getMessage());
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void authenticate_disabledUser_throwsAuthException() {
        String username = "disabledUser";
        User disabledUser = new User();
        disabledUser.setEnabled(false);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(disabledUser));

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.authenticate(username, "anyPassword"));

        assertEquals("Аккаунт отключен", exception.getMessage());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void authenticate_passwordHashIsNull_throwsAuthException() {
        String username = "noPasswordUser";
        User user = new User();
        user.setEnabled(true);
        user.setPasswordHash(null);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.authenticate(username, "anyPassword"));

        assertEquals("Пароль не установлен", exception.getMessage());
    }

    @Test
    void authenticate_wrongPassword_throwsAuthException() {
        String username = "user";
        String encodedPassword = "encoded";
        User user = new User();
        user.setEnabled(true);
        user.setPasswordHash(encodedPassword);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", encodedPassword))
                .thenReturn(false);

        AuthException exception = assertThrows(AuthException.class, () -> authService.authenticate(username, "wrong"));

        assertEquals("Неверный пароль", exception.getMessage());
        verify(passwordEncoder).matches("wrong", encodedPassword);
    }

    @Test
    void existsByUsername_userExists_returnsTrue() {
        String username = "existing";

        when(userRepository.existsByUsername(username))
                .thenReturn(true);

        boolean result = authService.existsByUsername(username);

        assertTrue(result);
        verify(userRepository).existsByUsername(username);
    }

    @Test
    void existsByUsername_userNotExists_returnsFalse() {
        String username = "nonExisting";

        when(userRepository.existsByUsername(username))
                .thenReturn(false);

        boolean result = authService.existsByUsername(username);

        assertTrue(!result);
        verify(userRepository).existsByUsername(username);
    }
}

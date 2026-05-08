package ru.laspace.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.laspace.auth.config.JwtProperties;
import ru.laspace.auth.dto.request.LoginRequest;
import ru.laspace.auth.dto.request.RefreshTokenRequest;
import ru.laspace.auth.dto.response.JwtResponse;
import ru.laspace.auth.exception.AuthException;
import ru.laspace.auth.security.JwtAuthenticationFilter;
import ru.laspace.auth.security.JwtService;
import ru.laspace.auth.security.UserDetailsServiceImpl;
import ru.laspace.auth.service.AuthService;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    }
)
@Import({GlobalExceptionHandler.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private JwtProperties jwtProperties;

    // --- POST /api/auth/login ---

    @Test
    void login_validCredentials_returns200() throws Exception {
        JwtResponse response = JwtResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .username("igor")
                .firstName("Игорь")
                .lastName("Тестов")
                .roles(Set.of("ROLE_OPERATOR"))
                .build();

        when(authService.login(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("igor", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.username").value("igor"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any(), any()))
                .thenThrow(new AuthException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("igor", "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_lockedAccount_returns401() throws Exception {
        when(authService.login(any(), any()))
                .thenThrow(new AuthException("Account is locked"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("igor", "password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"igor\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/auth/refresh ---

    @Test
    void refresh_validToken_returns200() throws Exception {
        JwtResponse response = JwtResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("refresh-token")
                .username("igor")
                .firstName("Игорь")
                .lastName("Тестов")
                .roles(Set.of("ROLE_OPERATOR"))
                .build();

        when(authService.refresh(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest("refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        when(authService.refresh(any()))
                .thenThrow(new AuthException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest("bad-token"))))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/auth/logout ---

    @Test
    void logout_validToken_returns200() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest("refresh-token"))))
                .andExpect(status().isOk());

        verify(authService).logout("refresh-token");
    }
}
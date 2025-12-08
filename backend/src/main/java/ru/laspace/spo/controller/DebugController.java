package ru.laspace.spo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.security.UserDetailsServiceImpl;

@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/test-auth")
    public ResponseEntity<?> testAuthentication(@RequestParam String username,
            @RequestParam String password) {
        try {
            log.info("=== НАЧАЛО ТЕСТА АУТЕНТИФИКАЦИИ ===");

            // 1. Проверим UserDetailsService
            var userDetails = userDetailsService.loadUserByUsername(username);
            log.info("UserDetails загружен: {}", userDetails.getUsername());
            log.info("Password hash: {}", userDetails.getPassword());
            log.info("Password hash length: {}",
                    userDetails.getPassword() != null ? userDetails.getPassword().length() : 0);
            log.info("Authorities: {}", userDetails.getAuthorities());

            // 2. Проверим PasswordEncoder
            boolean passwordMatches = false;
            if (userDetails.getPassword() != null) {
                passwordMatches = passwordEncoder.matches(password, userDetails.getPassword());
            }
            log.info("Password matches: {}", passwordMatches);

            // 3. Проверим AuthenticationManager
            Map<String, Object> response = new HashMap<>();
            response.put("userDetailsLoaded", true);
            response.put("username", userDetails.getUsername());
            response.put("passwordMatches", passwordMatches);

            if (passwordMatches) {
                try {
                    var authToken = new UsernamePasswordAuthenticationToken(username, password);
                    var authentication = authenticationManager.authenticate(authToken);
                    log.info("Authentication успешна: {}", authentication.isAuthenticated());
                    response.put("authenticationSuccess", true);
                    response.put("authenticated", authentication.isAuthenticated());
                } catch (Exception e) {
                    log.error("AuthenticationManager error: ", e);
                    response.put("authenticationSuccess", false);
                    response.put("authenticationError", e.getClass().getName());
                    response.put("authenticationMessage", e.getMessage());
                }
            } else {
                response.put("authenticationSuccess", false);
                response.put("authenticationError", "Password mismatch");
            }

            log.info("=== КОНЕЦ ТЕСТА ===");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при тестировании: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getClass().getName());
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
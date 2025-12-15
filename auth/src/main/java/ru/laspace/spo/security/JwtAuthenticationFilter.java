package ru.laspace.spo.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtValidator jwtValidator;
    private final JwtAuthenticationProvider jwtAuthenticationProvider;

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = parseJwt(request);

            if (jwt != null && jwtValidator.validateToken(jwt)) {
                Authentication authentication = jwtAuthenticationProvider.getAuthentication(jwt);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Аутентификация установлена для пользователя: {}", authentication.getName());
            }
        } catch (Exception e) {
            log.error("Не удается установить пользователя: {}", e.getMessage());
            // Не выбрасываем исключение, чтобы запрос мог продолжиться (но без
            // аутентификации)
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars/") ||
                path.startsWith("/swagger-resources") ||
                path.equals("/swagger-ui.html") ||
                path.equals("/favicon.ico") ||
                path.equals("/actuator/health")) {
            return true;
        }

        if ("OPTIONS".equals(method)) {
            return true;
        }

        if (path.equals("/api/auth/login") && "POST".equals(method)) {
            return true;
        }

        return false;
    }
}
package com.priceforge.app.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security: authentication happens in {@link JwtAuthenticationFilter}, fine-grained
 * per-use-case role checks happen via {@code @PreAuthorize} at the application-service layer
 * (see ARCHITECTURE.md) — this config only gates "must be authenticated" at the HTTP boundary.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // /error must stay open: Boot's error handling re-enters the security chain via
                // an internal forward when a filter calls response.sendError(...), and that
                // second pass has no Authorization header context — leaving it secured turns
                // every sendError(403/401) into an overwritten 401 from this second challenge.
                .requestMatchers("/auth/token", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                .anyRequest().authenticated())
            // Without an explicit entry point, Spring Security falls back to a 403 for missing/
            // invalid tokens too — a stateless REST API should distinguish "not authenticated"
            // (401) from "authenticated but forbidden" (403, still the default AccessDeniedHandler).
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

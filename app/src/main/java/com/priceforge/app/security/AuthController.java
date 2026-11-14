package com.priceforge.app.security;

import com.priceforge.app.security.dto.LoginRequest;
import com.priceforge.app.security.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo/testing utility only — not one of funcionalidad.md's use cases. The functional spec
 * assumes a role is already resolved for the caller; it never defines how a token is issued, so
 * this exists solely to make the RBAC on UC-01..UC-15 exercisable end-to-end.
 *
 * <p>It hands a token for any role to anyone who asks, so it must never run in production: it is
 * on by default for local development and the {@code prod} profile switches it off (and
 * {@link ProductionSecurityGuard} refuses to start if it is still on).
 */
@ConditionalOnProperty(
    prefix = "priceforge.security.demo-token-endpoint", name = "enabled", havingValue = "true", matchIfMissing = true)
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth (demo utility)", description = "Not a functional use case — issues a JWT for demo/testing purposes only.")
public class AuthController {

    private final JwtService jwtService;
    private final long expirationSeconds;

    public AuthController(
            JwtService jwtService,
            @Value("${priceforge.security.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.jwtService = jwtService;
        this.expirationSeconds = expirationMinutes * 60;
    }

    @Operation(summary = "Demo utility: issue a JWT for a given username/role")
    @PostMapping("/token")
    public LoginResponse issueToken(@Valid @RequestBody LoginRequest request) {
        String token = jwtService.generateToken(request.username(), request.role());
        return new LoginResponse(token, "Bearer", expirationSeconds);
    }
}

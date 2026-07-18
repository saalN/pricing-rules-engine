package com.priceforge.app.security;

import com.priceforge.app.security.dto.LoginRequest;
import com.priceforge.app.security.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo/testing utility only — not one of funcionalidad.md's use cases. The functional spec
 * assumes a role is already resolved for the caller; it never defines how a token is issued, so
 * this exists solely to make the RBAC on UC-01..UC-15 exercisable end-to-end.
 */
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

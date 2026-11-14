package com.priceforge.app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fails fast, at startup, when the {@code prod} profile is active with the development security
 * shortcuts still in place: the committed default JWT secret (anyone could forge a token with it)
 * or the demo endpoint that issues tokens for any role without credentials. Refusing to start is
 * deliberate — a misconfigured deployment should be loud, not quietly open.
 */
@Component
@Profile("prod")
public class ProductionSecurityGuard {

    static final int MINIMUM_SECRET_LENGTH = 32;
    private static final String DEV_SECRET_MARKER = "local-dev-only";

    public ProductionSecurityGuard(
            @Value("${priceforge.security.jwt.secret}") String jwtSecret,
            @Value("${priceforge.security.demo-token-endpoint.enabled:true}") boolean demoTokenEndpointEnabled) {
        validate(jwtSecret, demoTokenEndpointEnabled);
    }

    static void validate(String jwtSecret, boolean demoTokenEndpointEnabled) {
        if (jwtSecret == null || jwtSecret.contains(DEV_SECRET_MARKER)) {
            throw new IllegalStateException(
                "Refusing to start with the 'prod' profile: priceforge.security.jwt.secret is the development "
                    + "default. Set JWT_SECRET to a long random value.");
        }
        if (jwtSecret.length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException(
                "Refusing to start with the 'prod' profile: priceforge.security.jwt.secret must be at least "
                    + MINIMUM_SECRET_LENGTH + " characters.");
        }
        if (demoTokenEndpointEnabled) {
            throw new IllegalStateException(
                "Refusing to start with the 'prod' profile: the demo token endpoint (/auth/token) issues a "
                    + "token for any role without credentials; set "
                    + "priceforge.security.demo-token-endpoint.enabled=false.");
        }
    }
}

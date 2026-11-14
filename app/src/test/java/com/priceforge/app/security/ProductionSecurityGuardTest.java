package com.priceforge.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProductionSecurityGuardTest {

    private static final String STRONG_SECRET = "a-long-random-secret-that-is-not-the-dev-one-0123456789";
    private static final String DEV_DEFAULT_SECRET = "priceforge-local-dev-only-hmac-secret-change-me-32b+";

    @Test
    void acceptsAStrongSecretWithTheDemoEndpointOff() {
        assertThatCode(() -> ProductionSecurityGuard.validate(STRONG_SECRET, false)).doesNotThrowAnyException();
    }

    @Test
    void rejectsTheCommittedDevelopmentSecret() {
        assertThatThrownBy(() -> ProductionSecurityGuard.validate(DEV_DEFAULT_SECRET, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET")
            .hasMessageNotContaining(DEV_DEFAULT_SECRET);
    }

    @Test
    void rejectsASecretThatIsTooShort() {
        assertThatThrownBy(() -> ProductionSecurityGuard.validate("short", false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least " + ProductionSecurityGuard.MINIMUM_SECRET_LENGTH);
    }

    @Test
    void rejectsAMissingSecret() {
        assertThatThrownBy(() -> ProductionSecurityGuard.validate(null, false))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsTheDemoTokenEndpointBeingEnabled() {
        assertThatThrownBy(() -> ProductionSecurityGuard.validate(STRONG_SECRET, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("/auth/token");
    }

    @Test
    void theApplicationFailsToStartInTheProdProfileWithTheDevelopmentDefaults() {
        new ApplicationContextRunner()
            .withUserConfiguration(ProductionSecurityGuard.class)
            .withPropertyValues(
                "spring.profiles.active=prod",
                "priceforge.security.jwt.secret=" + DEV_DEFAULT_SECRET,
                "priceforge.security.demo-token-endpoint.enabled=true")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void theGuardIsNotActiveOutsideTheProdProfile() {
        new ApplicationContextRunner()
            .withUserConfiguration(ProductionSecurityGuard.class)
            .withPropertyValues(
                "priceforge.security.jwt.secret=" + DEV_DEFAULT_SECRET,
                "priceforge.security.demo-token-endpoint.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(ProductionSecurityGuard.class).hasNotFailed());
    }
}

package com.priceforge.app.security;

/**
 * The roles of the functional spec's permissions table. The external API client that calls UC-06
 * has no role of its own: any authenticated role except {@code AUDITOR} may calculate a price.
 */
public enum Role {
    ADMIN,
    REVENUE_MANAGER,
    MARKETING,
    AUDITOR
}

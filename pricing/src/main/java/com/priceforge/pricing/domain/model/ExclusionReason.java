package com.priceforge.pricing.domain.model;

/**
 * Why a rule that would otherwise apply was excluded from a calculation (UC-07).
 * {@code EXCLUDED_BY_HIGHER_PRIORITY} is normal precedence, not a conflict — only
 * {@code EXCLUDED_BY_TIEBREAK} (a genuine same-priority incompatibility) triggers
 * {@code RuleConflictDetected}.
 */
public enum ExclusionReason {
    EXCLUDED_BY_HIGHER_PRIORITY,
    EXCLUDED_BY_TIEBREAK
}

package com.priceforge.sharedkernel;

/**
 * Why an item that would otherwise apply was excluded from a calculation.
 * {@code EXCLUDED_BY_HIGHER_PRIORITY} is normal precedence, not a conflict — only
 * {@code EXCLUDED_BY_TIEBREAK} (a genuine same-priority incompatibility) is worth flagging as one.
 */
public enum ConflictExclusionReason {
    EXCLUDED_BY_HIGHER_PRIORITY,
    EXCLUDED_BY_TIEBREAK
}

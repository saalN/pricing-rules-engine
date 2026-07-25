package com.priceforge.sharedkernel;

/**
 * Shared by Pricing (rules) and Promotions (campaigns): both resolve conflicts between their own
 * items using the same priority/tie-break algorithm (see {@link ConflictResolver}), so the mode
 * driving that algorithm lives here rather than being duplicated per context.
 */
public enum CombinationMode {
    STACKABLE,
    EXCLUSIVE
}

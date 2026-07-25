package com.priceforge.sharedkernel;

import java.util.List;

/**
 * A set of items to be applied together, in priority order relative to other groups. A group
 * with more than one item is always a same-priority STACKABLE tie — the caller must apply every
 * item in the group against the same starting point (not chained sequentially), since chaining a
 * percentage and a fixed discount in different orders would give different results, breaking the
 * "order between ties doesn't affect the result" guarantee.
 */
public record ResolvedGroup<T>(List<T> items) {
}

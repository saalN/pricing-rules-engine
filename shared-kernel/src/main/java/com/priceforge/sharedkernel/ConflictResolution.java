package com.priceforge.sharedkernel;

import java.util.List;

public record ConflictResolution<T, ID>(List<ResolvedGroup<T>> groupsToApply, List<ExcludedItem<ID>> excluded) {
}

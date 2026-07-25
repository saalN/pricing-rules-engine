package com.priceforge.sharedkernel;

public record ExcludedItem<ID>(ID id, ConflictExclusionReason reason) {
}

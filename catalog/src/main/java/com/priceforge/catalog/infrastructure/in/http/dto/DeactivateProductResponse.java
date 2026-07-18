package com.priceforge.catalog.infrastructure.in.http.dto;

import java.util.UUID;

public record DeactivateProductResponse(UUID productId, String status) {
}

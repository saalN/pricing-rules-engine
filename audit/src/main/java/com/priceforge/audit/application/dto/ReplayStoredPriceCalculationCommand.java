package com.priceforge.audit.application.dto;

import java.util.UUID;

public record ReplayStoredPriceCalculationCommand(UUID eventId) {
}

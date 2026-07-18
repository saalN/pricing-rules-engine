package com.priceforge.app.security.dto;

import com.priceforge.app.security.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
    @NotBlank String username,
    @NotNull Role role
) {
}

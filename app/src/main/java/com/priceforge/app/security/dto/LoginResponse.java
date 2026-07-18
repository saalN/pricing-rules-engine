package com.priceforge.app.security.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}

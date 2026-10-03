package com.ecommerce.auth_service.dto;

public record AuthResponse(
        AccountDto account,
        String refreshToken,
        String accessToken
) {
}

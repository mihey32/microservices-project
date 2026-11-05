package com.ecommerce.auth_service.dto;

public record GitHubEmail(
        String email,
        boolean primary,
        boolean verified
) {
}

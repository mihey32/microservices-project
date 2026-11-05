package com.ecommerce.auth_service.dto;

import java.time.Instant;

public record UserCreatedEvent(
        Long userId,
        String email,
        String verificationToken,
        Instant createdAt
) {
}

package com.ecommerce.auth_service.dto;

import java.time.Instant;

public record EmailNotificationEvent(
        Long userId,
        String email,
        String resetToken,
        Instant createdAt
) {
}

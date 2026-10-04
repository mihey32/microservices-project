package com.ecommerce.auth_service.dto;

import java.time.Instant;

public record ErrorResponseDto(
        String message,
        String detailedMessage,
        Instant errorTime
) {
}

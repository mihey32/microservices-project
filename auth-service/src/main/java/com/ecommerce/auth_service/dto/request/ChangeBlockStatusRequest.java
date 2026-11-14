package com.ecommerce.auth_service.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChangeBlockStatusRequest(
        @NotNull
        Boolean blocked
) {
}

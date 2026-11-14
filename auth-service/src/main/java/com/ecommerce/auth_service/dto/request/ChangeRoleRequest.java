package com.ecommerce.auth_service.dto.request;

import com.ecommerce.auth_service.model.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull
        Role role
) {
}

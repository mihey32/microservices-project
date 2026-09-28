package com.ecommerce.auth_service.dto;

import com.ecommerce.auth_service.model.Role;
import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
public class AccountDto {
    private Long id;
    private String email;
    private Role role;
    private boolean enabled;
}

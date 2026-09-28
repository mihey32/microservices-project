package com.ecommerce.auth_service.dto;

import com.ecommerce.auth_service.model.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {
    private Long id;
    private String email;
    private Role role;
    private boolean enabled;
}

package com.ecommerce.auth_service.mapper;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.model.Account;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountMapper {
    public static AccountDto mapToDto(Account account) {
        AccountDto dto = new AccountDto();
        dto.setId(account.getId());
        dto.setEmail(account.getEmail());
        dto.setRole(account.getRole());
        dto.setEnabled(account.isEnabled());

        return dto;
    }
}

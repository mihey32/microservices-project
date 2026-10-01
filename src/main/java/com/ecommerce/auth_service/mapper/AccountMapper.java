package com.ecommerce.auth_service.mapper;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.model.Account;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountMapper {

    public AccountDto mapToDto(Account account) {
        return new AccountDto(
                account.getId(),
                account.getEmail(),
                account.getRole(),
                account.isBlocked(),
                account.isEmailVerified());
    }
}

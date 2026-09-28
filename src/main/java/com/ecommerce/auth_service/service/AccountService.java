package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.exceptions.ConflictException;
import com.ecommerce.auth_service.model.Account;
import com.ecommerce.auth_service.model.Role;
import com.ecommerce.auth_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountDto register(String email, String password) {
        if (accountRepository.existsByEmail(email)) {
            throw new ConflictException("email already exists");
        }

        String passwordHash = passwordEncoder.encode(password);
        Account createAccount = new Account();
        createAccount.setEmail(email);
        createAccount.setPasswordHash(passwordHash);
        createAccount.setRole(Role.USER);
        createAccount.setEnabled(false);

        Account savedAccount = accountRepository.save(createAccount);









            return null;
    }

    public AccountDto login(String email, String password) {
        return null;
    }

    public AccountDto oauthLogin(String oauth2Provider, String providerId, String email) {
        return null;
    }

}

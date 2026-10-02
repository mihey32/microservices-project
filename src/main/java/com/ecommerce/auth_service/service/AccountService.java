package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.exceptions.*;
import com.ecommerce.auth_service.mapper.AccountMapper;
import com.ecommerce.auth_service.model.Account;
import com.ecommerce.auth_service.model.Role;
import com.ecommerce.auth_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountDto register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String passwordHash = passwordEncoder.encode(password);

        Account createAccount = new Account();
        createAccount.setEmail(normalizedEmail);
        createAccount.setPasswordHash(passwordHash);
        createAccount.setRole(Role.ROLE_USER);

        try {
            Account savedAccount = accountRepository.saveAndFlush(createAccount);
            log.info("Account created: id={}", savedAccount.getId());
            return AccountMapper.mapToDto(savedAccount);
        } catch (DataIntegrityViolationException e) {
            log.info("Registration rejected: email already exists");
            throw new ConflictException("email already exists");
        }
    }

    @Transactional(readOnly = true)
    public AccountDto login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        Account signinAccount = findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.info("Invalid credentials attempt: email={}", normalizedEmail);
                    return new InvalidCredentialsException("invalid email or password");
                });

        if (signinAccount.getPasswordHash() == null
                || !passwordEncoder.matches(password, signinAccount.getPasswordHash())) {
            log.info("Invalid credentials attempt: email={}", normalizedEmail);
            throw new InvalidCredentialsException("invalid email or password");
        }

        if (signinAccount.isBlocked()) {
            log.info("Attempt to log in to a blocked account: email={}", normalizedEmail);
            throw new AccountBlockedException("account is blocked");
        }

        if (!signinAccount.isEmailVerified()) {
            log.info("Attempt to log in with unconfirmed email: email={}", normalizedEmail);
            throw new EmailNotConfirmedException("email is not verified");
        }

        return AccountMapper.mapToDto(signinAccount);
    }

    public AccountDto oauthLogin(String oauth2Provider, String providerId, String email) {
        return null;
    }

    private Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

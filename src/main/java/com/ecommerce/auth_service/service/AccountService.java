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
import java.util.Objects;
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
    public AccountDto authenticate(String email, String password) {
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
        return AccountMapper.mapToDto(signinAccount);
    }

    public Optional<AccountDto> findAccountById(Long id) {
        return accountRepository.findById(id).map(AccountMapper::mapToDto);
    }

    @Transactional
    public void changePassword(Long accountId, String rawPassword) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.debug("Account not found: accountId={}", accountId);
                    return new InvalidResetTokenException("account not found");
                });
        String newPasswordHash = passwordEncoder.encode(rawPassword);
        account.setPasswordHash(newPasswordHash);
        log.debug("Password changed: accountId={}", accountId);
    }

    @Transactional
    public void markEmailVerified(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.debug("Account not found: accountId={}", accountId);
                    return new InvalidVerificationTokenException("account not found");
                });
        if (account.isEmailVerified()) {
            log.debug("Email already verified: accountId={}", accountId);
            return;
        }
        account.setEmailVerified(true);
        log.debug("Email verified: accountId={}", accountId);
    }

    @Transactional(readOnly = true)
    public boolean verifyPassword(Long accountId, String rawPassword) {
        Optional<Account> accountOptional = accountRepository.findById(accountId);
        if (accountOptional.isEmpty()) {
            return false;
        }

        Account account = accountOptional.get();
        if (account.getPasswordHash() == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, account.getPasswordHash());
    }

    @Transactional
    public AccountDto provisionOAuthAccount(String provider, String providerId,
                                            String email, boolean emailVerified) {
        Objects.requireNonNull(email, "email for OAuth is null");
        String normalizedEmail = normalizeEmail(email);
        Optional<Account> accountOptional = accountRepository.findByOauth2ProviderAndOauth2ProviderId(provider, providerId);

        if (accountOptional.isPresent()) {
            return AccountMapper.mapToDto(accountOptional.get());
        } else {
            Optional<Account> signingAccountOptional = findByEmail(normalizedEmail);
            if (signingAccountOptional.isEmpty()) {
                Account createAccount = new Account();
                createAccount.setEmail(normalizedEmail);
                createAccount.setPasswordHash(null);
                createAccount.setEmailVerified(emailVerified);
                createAccount.setOauth2Provider(provider);
                createAccount.setOauth2ProviderId(providerId);
                createAccount.setRole(Role.ROLE_USER);
                Account savedAccount = accountRepository.save(createAccount);
                log.info("Account created by {} with providerId = {}: accountId={}",
                        savedAccount.getOauth2Provider(), savedAccount.getOauth2ProviderId(), savedAccount.getId());
                return AccountMapper.mapToDto(savedAccount);
            }
            var signingAccount = signingAccountOptional.get();
            signingAccount.setOauth2Provider(provider);
            signingAccount.setOauth2ProviderId(providerId);
            return AccountMapper.mapToDto(signingAccount);
        }
    }


    public Optional<AccountDto> findAccountByEmail(String email) {
        return accountRepository.findByEmail(normalizeEmail(email)).map(AccountMapper::mapToDto);
    }


    private Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

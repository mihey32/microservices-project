package com.ecommerce.auth_service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.service.AccountService;
import com.ecommerce.auth_service.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class PasswordResetServiceTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private AccountService accountService;

    private AccountDto account;

    @BeforeEach
    void setUp() {
        String email = "prt-" + UUID.randomUUID() + "@mail.com";
        account = accountService.register(email, "qwerty1234");
    }

    @Test
    void consumeResetToken_shouldReturnAccountIdForValidToken() {
        String raw = passwordResetService.generateResetToken(account);

        Optional<Long> result = passwordResetService.consumeResetToken(raw);

        assertThat(result).contains(account.id());
    }

    @Test
    void consumeResetToken_shouldReturnEmptyOnSecondCall() {
        String raw = passwordResetService.generateResetToken(account);

        assertThat(passwordResetService.consumeResetToken(raw)).contains(account.id());
        assertThat(passwordResetService.consumeResetToken(raw)).isEmpty();
    }

    @Test
    void consumeResetToken_shouldReturnEmptyForUnknownToken() {
        assertThat(passwordResetService.consumeResetToken(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void invalidatePreviousTokens_shouldDeleteAllTokensForAccount() {
        String raw1 = passwordResetService.generateResetToken(account);
        String raw2 = passwordResetService.generateResetToken(account);

        passwordResetService.invalidatePreviousTokens(account.id());

        assertThat(passwordResetService.consumeResetToken(raw1)).isEmpty();
        assertThat(passwordResetService.consumeResetToken(raw2)).isEmpty();
    }

    @Test
    void invalidatePreviousTokens_shouldNotAffectOtherAccounts() {
        String rawForA = passwordResetService.generateResetToken(account);

        AccountDto accountB = accountService.register(
                "prt-b-" + UUID.randomUUID() + "@mail.com", "qwerty1234");
        String rawForB = passwordResetService.generateResetToken(accountB);

        passwordResetService.invalidatePreviousTokens(account.id());

        assertThat(passwordResetService.consumeResetToken(rawForA)).isEmpty();
        assertThat(passwordResetService.consumeResetToken(rawForB)).contains(accountB.id());
    }

}

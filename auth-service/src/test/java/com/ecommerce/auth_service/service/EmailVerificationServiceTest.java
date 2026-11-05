package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.AbstractIntegrationTest;
import com.ecommerce.auth_service.dto.AccountDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class EmailVerificationServiceTest extends AbstractIntegrationTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private AccountService accountService;

    private AccountDto account;

    @BeforeEach
    void setUp() {
        String email = "evt-" + UUID.randomUUID() + "@mail.com";
        account = accountService.register(email, "qwerty1234");
    }

    @Test
    void consumeVerificationToken_shouldReturnAccountIdForValidToken() {
        String raw = emailVerificationService.generateVerificationToken(account);

        Optional<Long> result = emailVerificationService.consumeVerificationToken(raw);

        assertThat(result).contains(account.id());
    }

    @Test
    void consumeVerificationToken_shouldReturnEmptyOnSecondCall() {
        String raw = emailVerificationService.generateVerificationToken(account);

        assertThat(emailVerificationService.consumeVerificationToken(raw)).contains(account.id());
        assertThat(emailVerificationService.consumeVerificationToken(raw)).isEmpty();
    }

    @Test
    void consumeVerificationToken_shouldReturnEmptyForUnknownToken() {
        assertThat(emailVerificationService.consumeVerificationToken(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void invalidatePreviousTokens_shouldDeleteAllTokensForAccount() {
        String raw1 = emailVerificationService.generateVerificationToken(account);
        String raw2 = emailVerificationService.generateVerificationToken(account);

        emailVerificationService.invalidatePreviousTokens(account.id());

        assertThat(emailVerificationService.consumeVerificationToken(raw1)).isEmpty();
        assertThat(emailVerificationService.consumeVerificationToken(raw2)).isEmpty();
    }

    @Test
    void invalidatePreviousTokens_shouldNotAffectOtherAccounts() {
        String rawForA = emailVerificationService.generateVerificationToken(account);

        AccountDto accountB = accountService.register(
                "evt-b-" + UUID.randomUUID() + "@mail.com", "qwerty1234");
        String rawForB = emailVerificationService.generateVerificationToken(accountB);

        emailVerificationService.invalidatePreviousTokens(account.id());

        assertThat(emailVerificationService.consumeVerificationToken(rawForA)).isEmpty();
        assertThat(emailVerificationService.consumeVerificationToken(rawForB)).contains(accountB.id());
    }

}

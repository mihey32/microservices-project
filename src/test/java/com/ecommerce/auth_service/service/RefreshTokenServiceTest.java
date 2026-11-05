package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.AbstractIntegrationTest;
import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.model.RefreshToken;
import com.ecommerce.auth_service.repository.RefreshTokenRepository;
import com.ecommerce.auth_service.util.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Transactional
public class RefreshTokenServiceTest extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TokenHasher tokenHasher;

    private AccountDto account;

    @BeforeEach
    void setUp() {
        String email = "rt-" + UUID.randomUUID() + "@mail.com";
        account = accountService.register(email, "qwerty1234");
    }

    @Test
    void generateRefreshToken_shouldReturnRawUuidAndStoreHash() {
        String raw = refreshTokenService.generateRefreshToken(account);
        String expectedHash = tokenHasher.hashToken(raw);

        assertThatCode(() -> UUID.fromString(raw)).doesNotThrowAnyException();
        assertThat(expectedHash).hasSize(64);

        var saved = refreshTokenRepository.findByTokenHash(expectedHash).orElseThrow();
        assertThat(saved.getAccountId()).isEqualTo(account.id());
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getTokenHash()).isNotEqualTo(raw);
    }

    @Test
    void generateRefreshToken_shouldSetExpirationAround7Days() {
        Instant before = Instant.now();
        String raw = refreshTokenService.generateRefreshToken(account);

        var saved = refreshTokenRepository.findByTokenHash(tokenHasher.hashToken(raw)).orElseThrow();
        assertThat(saved.getExpiresAt()).isAfter(before.plus(Duration.ofDays(6)));
        assertThat(saved.getExpiresAt()).isBefore(before.plus(Duration.ofDays(8)));
    }

    @Test
    void consume_shouldReturnAccountIdForValidToken() {
        String raw = refreshTokenService.generateRefreshToken(account);

        Optional<Long> result = refreshTokenService.consume(raw);

        assertThat(result).contains(account.id());
    }

    @Test
    void consume_shouldReturnEmptyOnSecondCall() {
        String raw = refreshTokenService.generateRefreshToken(account);

        assertThat(refreshTokenService.consume(raw)).contains(account.id());
        assertThat(refreshTokenService.consume(raw)).isEmpty();
    }

    @Test
    void consume_shouldReturnEmptyForExpiredToken() {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken expired = new RefreshToken();
        expired.setTokenHash(tokenHasher.hashToken(rawToken));
        expired.setAccountId(account.id());
        expired.setExpiresAt(Instant.now().minus(Duration.ofHours(1)));
        expired.setRevoked(false);
        refreshTokenRepository.save(expired);

        assertThat(refreshTokenService.consume(rawToken)).isEmpty();
    }

    @Test
    void consume_shouldReturnEmptyForUnknownToken() {
        assertThat(refreshTokenService.consume(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void revokeByToken_shouldMakeTokenUnusable() {
        String raw = refreshTokenService.generateRefreshToken(account);

        refreshTokenService.revokeByToken(raw);

        assertThat(refreshTokenService.consume(raw)).isEmpty();
    }

    @Test
    void revokeByToken_shouldBeIdempotent() {
        String raw = refreshTokenService.generateRefreshToken(account);

        refreshTokenService.revokeByToken(raw);
        refreshTokenService.revokeByToken(raw);

        assertThat(refreshTokenService.consume(raw)).isEmpty();
    }

    @Test
    void revokeAllByAccountId_shouldRevokeOnlyTargetAccount() {
        String rawForA = refreshTokenService.generateRefreshToken(account);

        String emailB = "rt-b-" + UUID.randomUUID() + "@mail.com";
        AccountDto accountB = accountService.register(emailB, "qwerty1234");
        String rawForB = refreshTokenService.generateRefreshToken(accountB);

        refreshTokenService.revokeAllByAccountId(account.id());

        assertThat(refreshTokenService.consume(rawForA)).isEmpty();
        assertThat(refreshTokenService.consume(rawForB)).contains(accountB.id());
    }
}

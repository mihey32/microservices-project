package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.model.RefreshToken;
import com.ecommerce.auth_service.repository.RefreshTokenRepository;
import com.ecommerce.auth_service.util.TokenHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenService {

    private final TokenHasher tokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshExpiration;

    public RefreshTokenService(TokenHasher tokenHasher,
                               RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt.refresh.expiration}") Duration refreshExpiration) {
        this.tokenHasher = tokenHasher;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateRefreshToken(AccountDto account) {
        String token = UUID.randomUUID().toString();
        String hashedToken = tokenHasher.hashToken(token);

        RefreshToken createRefreshToken = new RefreshToken();
        createRefreshToken.setTokenHash(hashedToken);
        createRefreshToken.setAccountId(account.id());
        createRefreshToken.setExpiresAt(Instant.now().plus(refreshExpiration));

        refreshTokenRepository.save(createRefreshToken);
        return token;
    }

    @Transactional
    public Optional<Long> consume(String token) {
        String hash = tokenHasher.hashToken(token);

        int updated = refreshTokenRepository.consumeIfActive(hash, Instant.now());
        if (updated == 0) {
            log.debug("Refresh token invalid, expired or already consumed");
            return Optional.empty();
        }
        return refreshTokenRepository.findAccountIdByTokenHash(hash);
    }

    @Transactional
    public void revokeByToken(String rawToken) {
        String hash = tokenHasher.hashToken(rawToken);
        int revoked = refreshTokenRepository.revokeByTokenHash(hash);

        if (revoked == 0) {
            log.warn("Attempt to revoke non-existent or already revoked token");
        } else {
            log.debug("Refresh token revoked");
        }
    }

    @Transactional
    public void revokeAllByAccountId(Long accountId) {
        int revoked = refreshTokenRepository.revokeAllByAccountId(accountId);
        log.info("Revoked {} refresh tokens for account with id={}", revoked, accountId);
    }
}

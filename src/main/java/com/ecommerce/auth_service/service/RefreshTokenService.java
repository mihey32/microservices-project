package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.model.RefreshToken;
import com.ecommerce.auth_service.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshExpiration;


    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt.refresh.expiration}") Duration refreshExpiration) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateRefreshToken(AccountDto account) {
        String token = UUID.randomUUID().toString();
        String hashedToken = hashToken(token);

        RefreshToken createRefreshToken = new RefreshToken();
        createRefreshToken.setTokenHash(hashedToken);
        createRefreshToken.setAccountId(account.id());
        createRefreshToken.setExpiresAt(Instant.now().plus(refreshExpiration));

        refreshTokenRepository.save(createRefreshToken);
        return token;
    }

    @Transactional
    public Optional<Long> consume(String token) {
        String hash = hashToken(token);

        int updated = refreshTokenRepository.consumeIfActive(hash, Instant.now());
        if (updated == 0) {
            log.debug("Refresh token invalid, expired or already consumed");
            return Optional.empty();
        }
        return refreshTokenRepository.findAccountIdByTokenHash(hash);
    }

    @Transactional
    public void revokeByToken(String rawToken) {
        String hash = hashToken(rawToken);
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


    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing", e);
        }
    }
}

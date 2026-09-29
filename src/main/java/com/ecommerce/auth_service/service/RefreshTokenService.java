package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.model.Account;
import com.ecommerce.auth_service.model.RefreshToken;
import com.ecommerce.auth_service.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpiration;


    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt.refresh.expiration}") long refreshExpiration) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateRefreshToken(Account account) {
        String token = UUID.randomUUID().toString();
        String hashedToken = hashToken(token);

        RefreshToken createRefreshToken = new RefreshToken();
        createRefreshToken.setToken(hashedToken);
        createRefreshToken.setAccount(account);
        createRefreshToken.setExpiresAt(LocalDateTime.now().plus(refreshExpiration, ChronoUnit.MILLIS));
        createRefreshToken.setRevoked(false);

        refreshTokenRepository.save(createRefreshToken);
        log.info("сгенерирован refresh token");
        return token;
    }

    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepository.findByToken(hashToken(token))
                .filter(refreshToken -> !refreshToken.isRevoked())
                .filter(refreshToken -> refreshToken.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    public void revokeRefreshToken(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        log.info("refresh token аннулирован у пользователя с id = {}", refreshToken.getAccount().getId());
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

package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.model.PasswordResetToken;
import com.ecommerce.auth_service.repository.PasswordResetTokenRepository;
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
public class PasswordResetService {

    private final TokenHasher tokenHasher;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Duration resetTokenExpiration;

    public PasswordResetService(TokenHasher tokenHasher,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                @Value("${jwt.password-reset.expiration}") Duration resetTokenExpiration) {
        this.tokenHasher = tokenHasher;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.resetTokenExpiration = resetTokenExpiration;
    }

    public String generateResetToken(AccountDto account) {
        String token = UUID.randomUUID().toString();
        String hashedToken = tokenHasher.hashToken(token);

        PasswordResetToken createResetToken = new PasswordResetToken();
        createResetToken.setAccountId(account.id());
        createResetToken.setTokenHash(hashedToken);
        createResetToken.setExpiresAt(Instant.now().plus(resetTokenExpiration));

        passwordResetTokenRepository.save(createResetToken);

        return token;
    }

    @Transactional
    public Optional<Long> consumeResetToken(String rawToken) {
        String hash = tokenHasher.hashToken(rawToken);

        int updated = passwordResetTokenRepository.consumeIfActive(hash, Instant.now());
        if (updated == 0) {
            log.debug("Reset token invalid, expired or already used");
            return Optional.empty();
        }

        return passwordResetTokenRepository.findAccountIdByTokenHash(hash);
    }

    @Transactional
    public void invalidatePreviousTokens(Long accountId) {
        int deleted = passwordResetTokenRepository.deleteAllByAccountId(accountId);
        log.info("Deleted {} reset tokens for account with id = {}", deleted, accountId);
    }

}

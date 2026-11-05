package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.model.EmailVerificationToken;
import com.ecommerce.auth_service.repository.EmailVerificationTokenRepository;
import com.ecommerce.auth_service.util.TokenHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EmailVerificationService {
    private final TokenHasher tokenHasher;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    private final Duration verificationTokenExpiration;

    public EmailVerificationService(TokenHasher tokenHasher,
                                    EmailVerificationTokenRepository emailVerificationTokenRepository,
                                    @Value("${jwt.email-verification.expiration:24h}") Duration verificationTokenExpiration) {
        this.tokenHasher = tokenHasher;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.verificationTokenExpiration = verificationTokenExpiration;
    }

    public String generateVerificationToken(AccountDto account) {
        String token = UUID.randomUUID().toString();
        String hashedToken = tokenHasher.hashToken(token);

        EmailVerificationToken createVerificationToken = new EmailVerificationToken();
        createVerificationToken.setAccountId(account.id());
        createVerificationToken.setTokenHash(hashedToken);
        createVerificationToken.setExpiresAt(Instant.now().plus(verificationTokenExpiration));

        emailVerificationTokenRepository.save(createVerificationToken);

        return token;
    }

    @Transactional
    public Optional<Long> consumeVerificationToken(String rawToken) {
        String hash = tokenHasher.hashToken(rawToken);

        int updated = emailVerificationTokenRepository.consumeIfActive(hash, Instant.now());
        if (updated == 0) {
            log.debug("email verification token invalid, expired or already used");
            return Optional.empty();
        }

        return emailVerificationTokenRepository.findAccountIdByTokenHash(hash);
    }

    @Transactional
    public void invalidatePreviousTokens(Long accountId) {
        int deleted = emailVerificationTokenRepository.deleteAllByAccountId(accountId);
        log.info("Deleted {} email verification tokens for account with id = {}", deleted, accountId);
    }

}

package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    int deleteAllByAccountId(Long accountId);

    @Modifying
    @Query("update EmailVerificationToken evt set evt.used=true " +
            "where evt.tokenHash = :hash and evt.used = false and evt.expiresAt > :now")
    int consumeIfActive(@Param("hash") String hash, @Param("now") Instant now);

    @Query("select evt.accountId from EmailVerificationToken evt where evt.tokenHash = :hash")
    Optional<Long> findAccountIdByTokenHash(@Param("hash") String hash);

}

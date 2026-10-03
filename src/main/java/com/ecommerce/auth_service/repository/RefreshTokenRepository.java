package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true " +
            "where rt.tokenHash = :hash and rt.revoked = false")
    int revokeByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true " +
            "where rt.accountId = :accountId and  rt.revoked = false")
    int revokeAllByAccountId(@Param("accountId") Long accountId);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true " +
    "where rt.tokenHash = :hash and rt.revoked = false and rt.expiresAt > :now")
    int consumeIfActive(String hash, Instant now);

    @Modifying
    @Query("select rt.accountId from RefreshToken rt where rt.tokenHash = :hash")
    Optional<Long> findAccountIdByTokenHash(String hash);

}

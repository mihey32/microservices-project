package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    int deleteAllByAccountId(Long accountId);

    @Modifying
    @Query("update PasswordResetToken prt set prt.used=true " +
            "where prt.tokenHash = :hash and prt.used=false and prt.expiresAt > :now")
    int consumeIfActive(@Param("hash") String hash, @Param("now") Instant now);

    @Query("select prt.accountId from PasswordResetToken prt where prt.tokenHash = :hash")
    Optional<Long> findAccountIdByTokenHash(@Param("hash") String tokenHash);

}
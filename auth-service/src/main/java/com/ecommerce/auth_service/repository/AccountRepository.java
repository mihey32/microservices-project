package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);

    Optional<Account> findByOauth2ProviderAndOauth2ProviderId(String oauth2Provider, String oauth2ProviderId);
}

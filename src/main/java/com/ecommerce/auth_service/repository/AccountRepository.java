package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByEmail(String email);

}

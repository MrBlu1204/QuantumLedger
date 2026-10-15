package com.quantumledger.app.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {
    Optional<Account> findByUserIdAndCurrency(Long userId, String currency);
    Optional<Account> findByAccountCode(String accountCode);

}

package com.quantumledger.app.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {
    Optional<Account> findByUserIdAndCurrency(Long userId, String currency);

    Optional<Account> findByAccountCode(String accountCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountId = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT a.accountId FROM Account a WHERE a.userId = :userId AND a.currency = :currency")
    Optional<Long> findAccountId(@Param("userId") Long userId, @Param("currency") String currency);

}

package com.quantumledger.app.addmoney;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AddMoneyRequestRepository extends JpaRepository<AddMoneyRequest,Long> {

    /** SELECT ... FOR UPDATE: whoever holds the lock settles the request, everyone else waits. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AddMoneyRequest r where r.addMoneyId = :id")
    Optional<AddMoneyRequest> findByIdForUpdate(@Param("id") Long id);
}

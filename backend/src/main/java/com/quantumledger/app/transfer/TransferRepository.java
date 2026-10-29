package com.quantumledger.app.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByTransferRef(String transferRef);

    /**
     * One page of a user's transfers (sent or received), newest first.
     * Keyset pagination: only rows with transfer_id < :before.
     * The two sides are each index-ordered, then merged; UNION ALL is safe
     * because a transfer can never have sender == receiver (CHECK constraint).
     * Callers pass limit + 1 as :fetch so they can tell if another page exists.
     */
    @Query(value = """
            (SELECT * FROM transfers
                WHERE fk_sender_id = :userId AND transfer_id < :before
                ORDER BY transfer_id DESC LIMIT :fetch)
             UNION ALL
             (SELECT * FROM transfers
                WHERE fk_receiver_id = :userId AND transfer_id < :before
                ORDER BY transfer_id DESC LIMIT :fetch)
                ORDER BY transfer_id DESC LIMIT :fetch
                """, nativeQuery = true)
    List<Transfer> findStatementPage(@Param("userId") long userId,
                                     @Param("before") long before,
                                     @Param("fetch") int fetch);
}

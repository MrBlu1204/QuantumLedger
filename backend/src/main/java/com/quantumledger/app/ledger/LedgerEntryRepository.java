package com.quantumledger.app.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {


    /**
     * Credits minus debits for one account, in minor units (paise).
     * For wallet and income accounts this is the balance. For asset accounts the
     * balance is the negation of this, and that interpretation lives in a service.
     */

    @Query("""
            select coalesce(sum(case when e.direction = com.quantumLedger.app.ledger.Direction.CREDIT
                                     then e.amountMinor 
                                     else -e.amountMinor 
                                     end), OL)
            from LedgerEntry e
            where e.accountId = :accountId
       """)
    Long netCredits(@Param("accountId") Long accountId);
}

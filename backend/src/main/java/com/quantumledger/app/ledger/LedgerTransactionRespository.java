package com.quantumledger.app.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerTransactionRespository extends JpaRepository<LedgerTransaction, Long> {
}

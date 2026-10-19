package com.quantumledger.app.ledger;


import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "ledger_transactions")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_transaction_id")
    private Long ledgerTransactionId;

    @Column(name = "transaction_ref", nullable = false, updatable = false)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "transaction_type", nullable = false, updatable = false)
    private LedgerTransactionType transactionType;

    @Column(name = "reverses_transaction_id", updatable = false)
    private Long reversesTransactionId;

    @Column(updatable = false)
    private String description;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Instant createTime;

    protected LedgerTransaction() {}

    public LedgerTransaction(LedgerTransactionType type, String description) {
        this.transactionRef = UUID.randomUUID().toString();
        this.transactionType = type;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        this.createTime = Instant.now();
    }

    public Long getLedgerTransactionId() {
        return ledgerTransactionId;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public LedgerTransactionType getTransactionType() {
        return transactionType;
    }

    public Instant getCreateTime() {
        return createTime;
    }
}

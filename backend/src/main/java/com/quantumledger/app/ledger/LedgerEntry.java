package com.quantumledger.app.ledger;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Immutable
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_entry_id")
    private Long ledgerEntryId;

    @Column(name = "fk_ledger_transaction_id", nullable = false, updatable = false)
    private Long ledgerTransactionId;

    @Column(name = "fk_account_id", nullable = false, updatable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "direction", nullable = false, updatable = false)
    private Direction direction;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @Column(nullable = false, updatable = false,length = 3)
    private String currency;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Instant createTime;

    protected LedgerEntry() {}

    public LedgerEntry(Long ledgerTransactionId, Long accountId, Direction direction, long amountMinor, String currency) {

        if(amountMinor <= 0) {
            throw new IllegalArgumentException("Ledger entry amount must be positive");
        }

        this.ledgerTransactionId = ledgerTransactionId;
        this.accountId = accountId;
        this.direction = direction;
        this.amountMinor = amountMinor;
        this.currency = currency;
    }

    @PrePersist
    void onCreate() {
        this.createTime = Instant.now();
    }

    public Long getLedgerEntryId() {
        return ledgerEntryId;
    }

    public Long getLedgerTransactionId() {
        return ledgerTransactionId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Direction getDirection() {
        return direction;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreateTime() {
        return createTime;
    }
}

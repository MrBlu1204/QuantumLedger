package com.quantumledger.app.account;


import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "fk_user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "account_code")
    private String accountCode;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "create_time", nullable = false, updatable = false)
    private Instant createTime;

    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

    protected Account() {}

    public static Account wallet(Long userId, String currency) {
        if(userId == null) {
            throw new IllegalArgumentException("A wallet must belong to a user");
        }
        Account account = new Account();
        account.userId = userId;
        account.accountType = AccountType.WALLET;
        account.currency = currency;
        return account;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updateTime = Instant.now();
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getUserId() {
        return userId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceMinor() {return balanceMinor; }

    public String getStatus() {
        return status;
    }

    public Instant getCreateTime() {
        return createTime;
    }
}

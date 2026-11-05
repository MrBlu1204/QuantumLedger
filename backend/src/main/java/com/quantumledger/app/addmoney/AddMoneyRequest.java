package com.quantumledger.app.addmoney;

import com.quantumledger.app.payment.PaymentMethod;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "add_money_requests")
public class AddMoneyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "add_money_id")
    private Long addMoneyId;

    @Column(name = "request_ref", nullable = false, updatable = false)
    private String requestRef;

    @Column(name = "fk_user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "fk_account_id", nullable = false, updatable = false)
    private Long accountId;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, updatable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private AddMoneyStatus status;

    @Column(name = "gateway_ref")
    private String gatewayRef;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "fk_transaction_id")
    private Long ledgerTransactionId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Instant createTime;

    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

    protected AddMoneyRequest() { }

    public static AddMoneyRequest pending(Long userId, Long accountId,
                                          long amountMinor, String currency, PaymentMethod method) {
        if(userId == null || accountId == null || currency == null || method == null) {
            throw new IllegalArgumentException("add-money request is missing required values(s)");
        }

        if(amountMinor <= 0) {
            throw new IllegalArgumentException("add-money amount must be positive");
        }

        AddMoneyRequest request = new AddMoneyRequest();
        request.requestRef = UUID.randomUUID().toString();
        request.userId = userId;
        request.accountId = accountId;
        request.amountMinor = amountMinor;
        request.currency = currency;
        request.method = method;
        request.status = AddMoneyStatus.PENDING;

        return request;
    }

    public void markSuccess(String gatewayRef, Long ledgerTransactionId) {
        requirePending();

        if(ledgerTransactionId == null) {
            throw new IllegalArgumentException("a successful request needs a ledger transaction");
        }

        this.status = AddMoneyStatus.SUCCESS;
        this.gatewayRef = gatewayRef;
        this.ledgerTransactionId = ledgerTransactionId;
    }

    public void markFailed(String gatewayRef, String failureReason) {
        requirePending();
        this.status = AddMoneyStatus.FAILED;
        this.gatewayRef = gatewayRef;
        this.failureReason = failureReason;
    }

    private void requirePending() {
        if(this.status != AddMoneyStatus.PENDING) {
            throw new IllegalStateException("add-money request is already " + status);
        }
    }

    @PrePersist
    void onCreate() {
        this.createTime = Instant.now();
        this.updateTime = this.createTime;
    }

    @PreUpdate
    void onUpdate() {
        this.updateTime = Instant.now();
    }


    public Long getAddMoneyId() {
        return addMoneyId;
    }

    public String getRequestRef() {
        return requestRef;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getMethod() {return method;}

    public long getAmountMinor() {
        return amountMinor;
    }

    public AddMoneyStatus getStatus() {
        return status;
    }

    public String getGatewayRef() {
        return gatewayRef;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Long getLedgerTransactionId() {
        return ledgerTransactionId;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }
}

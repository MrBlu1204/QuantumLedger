package com.quantumledger.app.transfer;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="transfer_id")
    private Long transferId;

    @Column(name = "transfer_ref", nullable = false, updatable = false)
    private String transferRef;

    @Column(name = "fk_sender_id", nullable = false, updatable = false)
    private Long senderId;

    @Column(name = "fk_receiver_id", nullable = false, updatable = false)
    private Long receiverId;

    @Column(name = "fk_sender_account_id", nullable = false, updatable = false)
    private Long senderAccountId;

    @Column(name = "fk_receiver_account_id", nullable = false, updatable = false)
    private Long receiverAccountId;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(name = "fk_transaction_id", nullable = false, updatable = false)
    private Long ledgerTransactionId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Instant createTime;


    protected Transfer() { }


    public static Transfer completed(Long senderId, Long receiverId,
                                     Long senderAccountId, Long receiverAccountId,
                                     long amountMinor, String currency,
                                     Long ledgerTransactionId) {
        if(senderId == null || receiverId == null
        || senderAccountId == null || receiverAccountId == null
        || currency == null || ledgerTransactionId == null) {
            throw new IllegalArgumentException("transfer is missing required value(s)");
        }

        if(amountMinor <= 0) {
            throw new IllegalArgumentException("transfer amount must be positive");
        }

        if(senderId.equals(receiverId)) {
            throw new IllegalArgumentException("a transfer needs two different users");
        }

        Transfer transfer = new Transfer();
        transfer.transferRef = UUID.randomUUID().toString();
        transfer.senderId = senderId;
        transfer.receiverId = receiverId;
        transfer.senderAccountId = senderAccountId;
        transfer.receiverAccountId = receiverAccountId;
        transfer.amountMinor = amountMinor;
        transfer.currency = currency;
        transfer.status = TransferStatus.COMPLETED;
        transfer.ledgerTransactionId = ledgerTransactionId;

        return transfer;
    }

    @PrePersist
    void onCreate() {
        this.createTime = Instant.now();
    }

    public Long getTransferId() {
        return transferId;
    }

    public String getTransferRef() {
        return transferRef;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public Long getSenderAccountId() {
        return senderAccountId;
    }

    public Long getReceiverAccountId() {
        return receiverAccountId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public Long getLedgerTransactionId() {
        return ledgerTransactionId;
    }

    public Instant getCreateTime() {
        return createTime;
    }
}

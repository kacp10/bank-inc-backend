package com.bankinc.transaction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String transactionId;

    @Column(length = 16, nullable = false)
    private String cardId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    protected Transaction() {}

    public Transaction(String transactionId, String cardId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
        this.status = TransactionStatus.APPROVED;
    }

    public String getTransactionId() { return transactionId; }
    public String getCardId() { return cardId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public TransactionStatus getStatus() { return status; }

    public void cancel() {
        status = TransactionStatus.CANCELLED;
    }
}

package com.bankinc.card;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @Column(length = 16, nullable = false, updatable = false)
    private String cardId;

    @Column(nullable = false)
    private Long productId;

    @Column(length = 120)
    private String holderName;

    @Column(nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status = CardStatus.INACTIVE;

    protected Card() {}

    public Card(String cardId, Long productId, LocalDate expirationDate) {
        this.cardId = cardId;
        this.productId = productId;
        this.expirationDate = expirationDate;
    }

    public String getCardId() { return cardId; }
    public Long getProductId() { return productId; }
    public String getHolderName() { return holderName; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public BigDecimal getBalance() { return balance; }
    public CardStatus getStatus() { return status; }

    public void activate() {
        if (status == CardStatus.BLOCKED) {
            throw new IllegalStateException("Blocked card cannot be activated");
        }
        status = CardStatus.ACTIVE;
    }

    public void block() {
        status = CardStatus.BLOCKED;
    }

    public void recharge(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        balance = balance.subtract(amount);
    }
}

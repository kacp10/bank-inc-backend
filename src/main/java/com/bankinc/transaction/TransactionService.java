package com.bankinc.transaction;

import com.bankinc.card.Card;
import com.bankinc.card.CardService;
import com.bankinc.card.CardStatus;
import com.bankinc.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransactionService {

    private final CardService cardService;
    private final TransactionRepository transactionRepository;

    public TransactionService(CardService cardService, TransactionRepository transactionRepository) {
        this.cardService = cardService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction purchase(String cardId, BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new BusinessException("Price must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        Card card = cardService.getCardForUpdate(cardId);
        validatePurchase(card, price);

        card.debit(price);
        Transaction transaction = new Transaction(UUID.randomUUID().toString(), cardId, price);
        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Transaction find(String transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("Transaction not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Transaction cancel(String cardId, String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionIdAndCardId(transactionId, cardId)
                .orElseThrow(() -> new BusinessException("Transaction not found", HttpStatus.NOT_FOUND));

        if (transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new BusinessException("Transaction is already cancelled", HttpStatus.CONFLICT);
        }

        Duration elapsed = Duration.between(transaction.getCreatedAt(), LocalDateTime.now());
        if (elapsed.toHours() >= 24) {
            throw new BusinessException("Transaction cannot be cancelled after 24 hours", HttpStatus.CONFLICT);
        }

        Card card = cardService.getCardForUpdate(cardId);
        card.recharge(transaction.getAmount());
        transaction.cancel();
        return transaction;
    }

    private void validatePurchase(Card card, BigDecimal price) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new BusinessException("Card must be active", HttpStatus.CONFLICT);
        }
        if (LocalDate.now().isAfter(card.getExpirationDate())) {
            throw new BusinessException("Card is expired", HttpStatus.CONFLICT);
        }
        if (card.getBalance().compareTo(price) < 0) {
            throw new BusinessException("Insufficient balance", HttpStatus.CONFLICT);
        }
    }
}

package com.bankinc.card;

import com.bankinc.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Map;

@Service
public class CardService {

    private final CardRepository repository;
    private final SecureRandom random = new SecureRandom();

    public CardService(CardRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Map<String, String> generateNumber(long productId) {
        validateProductId(productId);

        for (int attempt = 0; attempt < 20; attempt++) {
            String cardId = String.format("%06d%010d", productId, random.nextLong(10_000_000_000L));
            if (!repository.existsById(cardId)) {
                repository.save(new Card(cardId, productId, LocalDate.now().plusYears(3)));
                return Map.of("cardId", cardId);
            }
        }
        throw new BusinessException("Unable to generate a unique card number", HttpStatus.CONFLICT);
    }

    @Transactional
    public void activate(String cardId) {
        Card card = getCard(cardId);
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessException("Card is blocked", HttpStatus.CONFLICT);
        }
        card.activate();
    }

    @Transactional
    public void block(String cardId) {
        Card card = getCard(cardId);
        card.block();
    }

    @Transactional
    public void recharge(String cardId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Balance recharge must be greater than zero", HttpStatus.BAD_REQUEST);
        }
        Card card = getCardForUpdate(cardId);
        card.recharge(amount);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String cardId) {
        return getCard(cardId).getBalance();
    }

    public Card getCard(String cardId) {
        return repository.findById(cardId)
                .orElseThrow(() -> new BusinessException("Card not found", HttpStatus.NOT_FOUND));
    }

    public Card getCardForUpdate(String cardId) {
        return repository.findByIdForUpdate(cardId)
                .orElseThrow(() -> new BusinessException("Card not found", HttpStatus.NOT_FOUND));
    }

    private void validateProductId(long productId) {
        if (productId < 0 || productId > 999999) {
            throw new BusinessException("productId must contain up to 6 digits", HttpStatus.BAD_REQUEST);
        }
    }
}

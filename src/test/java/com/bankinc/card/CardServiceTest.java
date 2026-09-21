package com.bankinc.card;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;

import com.bankinc.common.BusinessException;

class CardServiceTest {
    CardRepository repository;
    CardService service;

    @BeforeEach
    void setUp() {
        repository = mock(CardRepository.class);
        service = new CardService(repository);
    }

    @Test
    void generatesSixDigitProductPrefixAndSixteenDigits() {
        when(repository.existsById(anyString())).thenReturn(false);

        var result = service.generateNumber(102030L);

        assertEquals(16, result.get("cardId").length());
        assertTrue(result.get("cardId").startsWith("102030"));
        verify(repository).save(any(Card.class));
    }

    @Test
    void acceptsShortProductIdAndPadsItToSixDigits() {
        when(repository.existsById(anyString())).thenReturn(false);

        var result = service.generateNumber(123L);

        assertEquals(16, result.get("cardId").length());
        assertTrue(result.get("cardId").startsWith("000123"));
    }

    @Test
    void rejectsProductIdOutsideAllowedRange() {
        BusinessException high = assertThrows(
                BusinessException.class,
                () -> service.generateNumber(1_000_000L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, high.getStatus());
        assertThrows(
                BusinessException.class,
                () -> service.generateNumber(-1L)
        );
    }

    @Test
    void failsWhenUniqueCardCannotBeGenerated() {
        when(repository.existsById(anyString())).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.generateNumber(102030L)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(repository, times(20)).existsById(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void activatesCard() {
        Card card = card();
        when(repository.findById(card.getCardId()))
                .thenReturn(Optional.of(card));

        service.activate(card.getCardId());

        assertEquals(CardStatus.ACTIVE, card.getStatus());
    }

    @Test
    void rejectsActivationOfBlockedCard() {
        Card card = card();
        card.block();

        when(repository.findById(card.getCardId()))
                .thenReturn(Optional.of(card));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.activate(card.getCardId())
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void blocksCard() {
        Card card = card();
        card.activate();

        when(repository.findById(card.getCardId()))
                .thenReturn(Optional.of(card));

        service.block(card.getCardId());

        assertEquals(CardStatus.BLOCKED, card.getStatus());
    }

    @Test
    void rechargesAndReadsBalance() {
        Card card = card();

        when(repository.findByIdForUpdate(card.getCardId()))
                .thenReturn(Optional.of(card));
        when(repository.findById(card.getCardId()))
                .thenReturn(Optional.of(card));

        service.recharge(
                card.getCardId(),
                new BigDecimal("100.50")
        );

        assertEquals(
                new BigDecimal("100.50"),
                service.getBalance(card.getCardId())
        );
    }

    @Test
    void rejectsNullZeroAndNegativeRecharge() {
        assertThrows(
                BusinessException.class,
                () -> service.recharge("1020301234567801", null)
        );

        assertThrows(
                BusinessException.class,
                () -> service.recharge(
                        "1020301234567801",
                        BigDecimal.ZERO
                )
        );

        assertThrows(
                BusinessException.class,
                () -> service.recharge(
                        "1020301234567801",
                        new BigDecimal("-1")
                )
        );

        verify(repository, never())
                .findByIdForUpdate(anyString());
    }

    @Test
    void getCardThrowsNotFound() {
        when(repository.findById("missing"))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.getCard("missing")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void getCardForUpdateThrowsNotFound() {
        when(repository.findByIdForUpdate("missing"))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.getCardForUpdate("missing")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    private Card card() {
        return new Card(
                "1020301234567801",
                102030L,
                LocalDate.now().plusYears(3)
        );
    }
}
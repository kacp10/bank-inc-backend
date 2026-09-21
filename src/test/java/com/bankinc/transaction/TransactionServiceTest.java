package com.bankinc.transaction;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;

import com.bankinc.card.Card;
import com.bankinc.card.CardService;
import com.bankinc.common.BusinessException;

class TransactionServiceTest {
    CardService cardService;
    TransactionRepository repository;
    TransactionService service;

    @BeforeEach
    void setUp() {
        cardService = mock(CardService.class);
        repository = mock(TransactionRepository.class);
        service = new TransactionService(cardService, repository);
    }

    @Test
    void purchaseDebitsBalanceAndCreatesTransaction() {
        Card card = activeCard(new BigDecimal("500"));

        when(cardService.getCardForUpdate(card.getCardId()))
                .thenReturn(card);
        when(repository.save(any(Transaction.class)))
                .thenAnswer(i -> i.getArgument(0));

        Transaction tx = service.purchase(
                card.getCardId(),
                new BigDecimal("100")
        );

        assertEquals(new BigDecimal("400"), card.getBalance());
        assertEquals(TransactionStatus.APPROVED, tx.getStatus());
        assertEquals(card.getCardId(), tx.getCardId());
        assertEquals(new BigDecimal("100"), tx.getAmount());
        assertNotNull(tx.getTransactionId());
        assertNotNull(tx.getCreatedAt());
    }

    @Test
    void purchaseRejectsNullZeroAndNegativePrice() {
        assertThrows(
                BusinessException.class,
                () -> service.purchase("1020301234567801", null)
        );

        assertThrows(
                BusinessException.class,
                () -> service.purchase(
                        "1020301234567801",
                        BigDecimal.ZERO
                )
        );

        assertThrows(
                BusinessException.class,
                () -> service.purchase(
                        "1020301234567801",
                        new BigDecimal("-1")
                )
        );

        verify(cardService, never())
                .getCardForUpdate(anyString());
    }

    @Test
    void purchaseRejectsInsufficientBalance() {
        Card card = activeCard(new BigDecimal("50"));

        when(cardService.getCardForUpdate(card.getCardId()))
                .thenReturn(card);

        assertThrows(
                BusinessException.class,
                () -> service.purchase(
                        card.getCardId(),
                        new BigDecimal("100")
                )
        );

        assertEquals(new BigDecimal("50"), card.getBalance());
        verify(repository, never()).save(any());
    }

    @Test
    void purchaseRejectsInactiveCard() {
        Card card = new Card(
                "1020301234567801",
                102030L,
                LocalDate.now().plusYears(3)
        );

        when(cardService.getCardForUpdate(card.getCardId()))
                .thenReturn(card);

        assertThrows(
                BusinessException.class,
                () -> service.purchase(
                        card.getCardId(),
                        BigDecimal.ONE
                )
        );
    }

    @Test
    void purchaseRejectsExpiredCard() {
        Card card = new Card(
                "1020301234567801",
                102030L,
                LocalDate.now().minusDays(1)
        );

        card.activate();
        card.recharge(new BigDecimal("500"));

        when(cardService.getCardForUpdate(card.getCardId()))
                .thenReturn(card);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.purchase(
                        card.getCardId(),
                        BigDecimal.ONE
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void findsTransaction() {
        Transaction tx = transaction();

        when(repository.findById("tx-1"))
                .thenReturn(Optional.of(tx));

        assertSame(tx, service.find("tx-1"));
    }

    @Test
    void findRejectsUnknownTransaction() {
        when(repository.findById("missing"))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.find("missing")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void cancelReturnsMoneyAndMarksTransaction() {
        Card card = activeCard(BigDecimal.ZERO);
        Transaction tx = transaction();

        when(repository.findByTransactionIdAndCardId(
                "tx-1",
                card.getCardId()
        )).thenReturn(Optional.of(tx));

        when(cardService.getCardForUpdate(card.getCardId()))
                .thenReturn(card);

        Transaction result = service.cancel(
                card.getCardId(),
                "tx-1"
        );

        assertEquals(
                TransactionStatus.CANCELLED,
                result.getStatus()
        );

        assertEquals(
                new BigDecimal("100"),
                card.getBalance()
        );
    }

    @Test
    void cancelRejectsUnknownTransaction() {
        when(repository.findByTransactionIdAndCardId(
                "missing",
                "1020301234567801"
        )).thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> service.cancel(
                        "1020301234567801",
                        "missing"
                )
        );
    }

    @Test
    void rejectsDuplicateCancellation() {
        Transaction tx = transaction();
        tx.cancel();

        when(repository.findByTransactionIdAndCardId(
                "tx-1",
                tx.getCardId()
        )).thenReturn(Optional.of(tx));

        assertThrows(
                BusinessException.class,
                () -> service.cancel(
                        tx.getCardId(),
                        "tx-1"
                )
        );

        verify(cardService, never())
                .getCardForUpdate(anyString());
    }

    @Test
    void rejectsCancellationAfterTwentyFourHours()
            throws Exception {

        Transaction tx = transaction();

        Field createdAt =
                Transaction.class.getDeclaredField("createdAt");

        createdAt.setAccessible(true);
        createdAt.set(
                tx,
                LocalDateTime.now().minusHours(25)
        );

        when(repository.findByTransactionIdAndCardId(
                "tx-1",
                tx.getCardId()
        )).thenReturn(Optional.of(tx));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.cancel(
                        tx.getCardId(),
                        "tx-1"
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(
                TransactionStatus.APPROVED,
                tx.getStatus()
        );

        verify(cardService, never())
                .getCardForUpdate(anyString());
    }

    private Card activeCard(BigDecimal balance) {
        Card card = new Card(
                "1020301234567801",
                102030L,
                LocalDate.now().plusYears(3)
        );

        card.activate();
        card.recharge(balance);

        return card;
    }

    private Transaction transaction() {
        return new Transaction(
                "tx-1",
                "1020301234567801",
                new BigDecimal("100")
        );
    }
}
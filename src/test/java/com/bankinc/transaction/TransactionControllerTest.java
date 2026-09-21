package com.bankinc.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    private TransactionService service;
    private TransactionController controller;

    @BeforeEach
    void setUp() {
        service = mock(TransactionService.class);
        controller = new TransactionController(service);
    }

    @Test
    void purchases() {
        Transaction transaction = transaction();

        when(service.purchase(
                "1020301234567801",
                new BigDecimal("100")
        )).thenReturn(transaction);

        var request =
                new TransactionController.PurchaseRequest(
                        "1020301234567801",
                        new BigDecimal("100")
                );

        var result = controller.purchase(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTransactionResponse(result.getBody());
    }

    @Test
    void findsTransaction() {
        Transaction transaction = transaction();

        when(service.find("tx-1")).thenReturn(transaction);

        var result = controller.find("tx-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTransactionResponse(result.getBody());
    }

    @Test
    void cancelsTransaction() {
        Transaction transaction = transaction();

        when(service.cancel(
                "1020301234567801",
                "tx-1"
        )).thenReturn(transaction);

        var request =
                new TransactionController.CancellationRequest(
                        "1020301234567801",
                        "tx-1"
                );

        var result = controller.cancel(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTransactionResponse(result.getBody());
    }

    private Transaction transaction() {
        return new Transaction(
                "tx-1",
                "1020301234567801",
                new BigDecimal("100")
        );
    }

    private void assertTransactionResponse(
            Map<String, Object> response) {

        assertEquals("tx-1", response.get("transactionId"));
        assertEquals(
                "1020301234567801",
                response.get("cardId")
        );
        assertEquals(
                new BigDecimal("100"),
                response.get("price")
        );
        assertEquals(
                TransactionStatus.APPROVED,
                response.get("status")
        );
    }
}
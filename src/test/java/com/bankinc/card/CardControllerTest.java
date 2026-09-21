package com.bankinc.card;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CardControllerTest {

    private CardService service;
    private CardController controller;

    @BeforeEach
    void setUp() {
        service = mock(CardService.class);
        controller = new CardController(service);
    }

    @Test
    void generatesCardNumber() {
        Map<String, String> response =
                Map.of("cardId", "1020301234567801");

        when(service.generateNumber(102030L))
                .thenReturn(response);

        var result = controller.generate(102030L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void activatesCard() {
        var request = new CardController.EnrollRequest(
                "1020301234567801"
        );

        var result = controller.enroll(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).activate("1020301234567801");
    }

    @Test
    void blocksCard() {
        var result = controller.block("1020301234567801");

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(service).block("1020301234567801");
    }

    @Test
    void rechargesCard() {
        var request = new CardController.RechargeRequest(
                "1020301234567801",
                new BigDecimal("100")
        );

        var result = controller.recharge(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());

        verify(service).recharge(
                "1020301234567801",
                new BigDecimal("100")
        );
    }

    @Test
    void returnsBalance() {
        when(service.getBalance("1020301234567801"))
                .thenReturn(new BigDecimal("250"));

        var result = controller.balance("1020301234567801");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(
                "1020301234567801",
                result.getBody().get("cardId")
        );
        assertEquals(
                new BigDecimal("250"),
                result.getBody().get("balance")
        );
    }
}
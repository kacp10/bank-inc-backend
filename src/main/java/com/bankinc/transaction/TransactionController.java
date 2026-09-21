package com.bankinc.transaction;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> purchase(@Valid @RequestBody PurchaseRequest request) {
        Transaction tx = service.purchase(request.cardId(), request.price());
        return ResponseEntity.ok(toResponse(tx));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Map<String, Object>> find(@PathVariable String transactionId) {
        return ResponseEntity.ok(toResponse(service.find(transactionId)));
    }

    @PostMapping("/anulation")
    public ResponseEntity<Map<String, Object>> cancel(@Valid @RequestBody CancellationRequest request) {
        return ResponseEntity.ok(toResponse(service.cancel(request.cardId(), request.transactionId())));
    }

    private Map<String, Object> toResponse(Transaction tx) {
        return Map.of(
                "transactionId", tx.getTransactionId(),
                "cardId", tx.getCardId(),
                "price", tx.getAmount(),
                "createdAt", tx.getCreatedAt(),
                "status", tx.getStatus()
        );
    }

    public record PurchaseRequest(
            @NotBlank @Pattern(regexp = "\\d{16}") String cardId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal price
    ) {}

    public record CancellationRequest(
            @NotBlank @Pattern(regexp = "\\d{16}") String cardId,
            @NotBlank String transactionId
    ) {}
}

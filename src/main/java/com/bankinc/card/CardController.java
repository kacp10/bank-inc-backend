package com.bankinc.card;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/card")
public class CardController {

    private final CardService service;

    public CardController(CardService service) {
        this.service = service;
    }

    @GetMapping("/{productId}/number")
    public ResponseEntity<Map<String, String>> generate(@PathVariable long productId) {
        return ResponseEntity.ok(service.generateNumber(productId));
    }

    @PostMapping("/enroll")
    public ResponseEntity<Void> enroll(@Valid @RequestBody EnrollRequest request) {
        service.activate(request.cardId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> block(@PathVariable String cardId) {
        service.block(cardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/balance")
    public ResponseEntity<Void> recharge(@Valid @RequestBody RechargeRequest request) {
        service.recharge(request.cardId(), request.balance());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/balance/{cardId}")
    public ResponseEntity<Map<String, Object>> balance(@PathVariable String cardId) {
        return ResponseEntity.ok(Map.of("cardId", cardId, "balance", service.getBalance(cardId)));
    }

    public record EnrollRequest(
            @NotBlank @Pattern(regexp = "\\d{16}") String cardId
    ) {}

    public record RechargeRequest(
            @NotBlank @Pattern(regexp = "\\d{16}") String cardId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal balance
    ) {}
}

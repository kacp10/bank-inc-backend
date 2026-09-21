package com.bankinc.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByTransactionIdAndCardId(String transactionId, String cardId);
}

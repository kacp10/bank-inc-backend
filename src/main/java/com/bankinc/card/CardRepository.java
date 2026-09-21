package com.bankinc.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c where c.cardId = :cardId")
    Optional<Card> findByIdForUpdate(String cardId);
}

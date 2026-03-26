package com.example.coopachat.repositories;

import com.example.coopachat.entities.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    /**
     * Dernière transition de statut d'une commande (la plus récente).
     * Sert à afficher "qui" a positionné le statut courant.
     */
    Optional<OrderStatusHistory> findTopByOrderIdOrderByChangedAtDesc(Long orderId);
}


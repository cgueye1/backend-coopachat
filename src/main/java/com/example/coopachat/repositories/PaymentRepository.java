package com.example.coopachat.repositories;

import com.example.coopachat.entities.Payment;
import com.example.coopachat.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Compte le nombre de paiements ayant le statut donné (sans filtre de date).
     * Utilisé pour le dashboard admin lorsque la période n'est plus appliquée.
     */
    long countByStatus(PaymentStatus status);

    /**
     * Compte le nombre de paiements ayant le statut donné, créés entre start et end.
     */
    long countByStatusAndCreatedAtBetween(PaymentStatus status, LocalDateTime start, LocalDateTime end);
}

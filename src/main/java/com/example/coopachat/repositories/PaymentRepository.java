package com.example.coopachat.repositories;

import com.example.coopachat.entities.Payment;
import com.example.coopachat.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Compte le nombre de paiements ayant le statut donné, créés entre start et end.
     * Utilisé pour le KPI "paiements échoués" et pour la liste "paiements par statut" du dashboard.
     */
    long countByStatusAndCreatedAtBetween(PaymentStatus status, LocalDateTime start, LocalDateTime end);
}

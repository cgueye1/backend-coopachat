package com.example.coopachat.repositories;

import com.example.coopachat.entities.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'entité Promotion.
 */
@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /** Vérifie s'il existe au moins une promotion active (une seule autorisée à la fois). */
    boolean existsByIsActiveTrue();
}

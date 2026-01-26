package com.example.coopachat.repositories;

import com.example.coopachat.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'entité Category (Catégorie de produit)
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Vérifie si une catégorie avec ce nom existe déjà
     */
    Boolean existsByName(String name);

    /**
     * Récupère les catégories liées à un coupon
     */
    List<Category> findByCouponId(Long couponId);

    // ============================================================================
    // 🏠 Accueil salarié
    // ============================================================================

    /**
     * Récupère les 4 dernières catégories (ordre par id)
     */
    List<Category> findTop4ByOrderByIdDesc();
}





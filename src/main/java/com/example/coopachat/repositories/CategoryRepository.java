package com.example.coopachat.repositories;

import com.example.coopachat.entities.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
     * Recherche des catégories par nom (insensible à la casse), triées par id décroissant.
     */
    List<Category> findByNameContainingIgnoreCaseOrderByIdDesc(String name);

    /**
     * Récupère les catégories liées à un coupon
     */
    List<Category> findByCouponId(Long couponId);

    // ============================================================================
    // 🏠 Accueil salarié / Catalogue salarié — uniquement catégories avec au moins 1 produit actif
    // ============================================================================

    /**
     * Récupère les 4 dernières catégories (ordre par id)
     */
    List<Category> findTop4ByOrderByIdDesc();

    /**
     * Catégories ayant au moins un produit actif (pour liste déroulante catalogue / accueil salarié).
     */
    @Query("SELECT DISTINCT c FROM Category c WHERE EXISTS (SELECT 1 FROM Product p WHERE p.category = c AND p.status = true)")
    List<Category> findCategoriesWithAtLeastOneActiveProduct();

    /**
     * Catégories ayant au moins un produit actif, triées par id décroissant (pour accueil, limiter via Pageable).
     */
    @Query("SELECT c FROM Category c WHERE EXISTS (SELECT 1 FROM Product p WHERE p.category = c AND p.status = true) ORDER BY c.id DESC")
    List<Category> findCategoriesWithActiveProductsOrderByIdDesc(Pageable pageable);
}





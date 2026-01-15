package com.example.coopachat.repositories;

import com.example.coopachat.entities.Category;
import com.example.coopachat.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'entité Product (Produit)
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Vérifie si un produit avec ce nom existe déjà
     */
    Boolean existsByName(String name);

    /**
     * Vérifie si un produit avec ce code existe déjà
     */
    Boolean existsByProductCode(String productCode);

    // ============================================================================
    // 🔍 RECHERCHE ET FILTRES
    // ============================================================================

    /**
     * Recherche par nom ou code produit (ignorer la casse)
     */
    Page<Product> findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCase(
            String name, String productCode, Pageable pageable);

    /**
     * Recherche par nom ou code produit + filtre par catégorie
     */
    Page<Product> findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategory(
            String name, String productCode, Category category, Pageable pageable);

    /**
     * Recherche par nom ou code produit + filtre par statut
     */
    Page<Product> findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndStatus(
            String name, String productCode, Boolean status, Pageable pageable);

    /**
     * Recherche par nom ou code produit + filtre par catégorie + filtre par statut
     */
    Page<Product> findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategoryAndStatus(
            String name, String productCode, Category category, Boolean status, Pageable pageable);

    /**
     * Filtre par catégorie uniquement
     */
    Page<Product> findByCategory(Category category, Pageable pageable);

    /**
     * Filtre par statut uniquement
     */
    Page<Product> findByStatus(Boolean status, Pageable pageable);


    /**
     * Filtre par catégorie + statut
     */
    Page<Product> findByCategoryAndStatus(Category category, Boolean status, Pageable pageable);


    /**
     * Compter les produits par statut (actif/inactif)
     */
     long countByStatus(Boolean status);
}


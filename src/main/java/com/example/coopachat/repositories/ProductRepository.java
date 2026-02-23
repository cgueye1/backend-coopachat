package com.example.coopachat.repositories;

import com.example.coopachat.entities.Category;
import com.example.coopachat.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
     * Recherche par nom produit + filtre par statut
     */
    Page<Product> findByNameContainingIgnoreCaseAndStatus(String name, Boolean status, Pageable pageable);

    /**
     * Recherche par nom ou code produit + filtre par catégorie + filtre par statut
     */
    Page<Product> findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategoryAndStatus(
            String name, String productCode, Category category, Boolean status, Pageable pageable);

    /**
     * Recherche par nom  + filtre par catégorie + filtre par statut
     */
    Page<Product> findByNameContainingIgnoreCaseAndCategoryAndStatus(
            String name, Category category, Boolean status, Pageable pageable);

    /**
     * Filtre par catégorie uniquement
     */
    Page<Product> findByCategory(Category category, Pageable pageable);

    /**
     * Filtre par statut uniquement
     */
    Page<Product> findByStatus(Boolean status, Pageable pageable);
    List <Product> findByStatus(Boolean status);


    /**
     * Filtre par catégorie + statut
     */
    Page<Product> findByCategoryAndStatus(Category category, Boolean status, Pageable pageable);

    // ============================================================================
    // 🏠 Accueil salarié
    // ============================================================================

    /**
     * Récupère les 4 derniers produits actifs
     */
    List<Product> findTop4ByStatusTrueOrderByCreatedAtDesc();

    /**
     * Récupère les produits liés à un coupon
     */
    List<Product> findByCouponId(Long couponId);

    /**
     * Récupère les produits appartenant à une liste de catégories
     */
    List<Product> findByCategoryIn(List<Category> categories);

    /**
     * Récupère les produits en alerte de stock (stock actuel < seuil minimum).
     * de façon simplifiée
     *
     * Cette requête permet un filtrage optionnel par :
     * - mot-clé de recherche (`search`) sur le nom ou le code du produit
     *   (recherche insensible à la casse grâce à LOWER)
     * - catégorie du produit
     *
     * Si un paramètre est NULL, le filtre correspondant est ignoré.
     * Les résultats sont retournés sous forme paginée.
     */

    @Query("""
        SELECT p FROM Product p
        WHERE p.currentStock < p.minThreshold
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:category IS NULL OR p.category = :category)
        """)
    Page<Product> findStockAlerts(
            @Param("search") String search,
            @Param("category") Category category,
            Pageable pageable
    );

    // ============================================================================
    // 🔍 Statistiques
    // ============================================================================

    /**
     * Compter les produits par statut (actif/inactif)
     */
     long countByStatus(Boolean status);

    /**
     * Compter les produits avec un stock exact (ex: 0 pour rupture)
     */
    long countByCurrentStock(Integer currentStock);

    /**
     * Compter les produits sous seuil (stock > 0 (car si stock = 0 ça sera une rupture )et stock < seuil)
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.currentStock > 0 AND p.currentStock < p.minThreshold")
    long countLowStock();


}


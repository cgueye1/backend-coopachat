package com.example.coopachat.repositories;

import com.example.coopachat.entities.Coupon;
import com.example.coopachat.enums.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Vérifie si un coupon existe déjà avec ce code.
     */
    boolean existsByCode(String code);

    /**
     * Vérifie si un coupon existe déjà avec ce nom.
     */
    boolean existsByName(String name);


    /**
     * Liste paginée des coupons avec filtres optionnels.
     */
    @Query("""
            SELECT c
            FROM Coupon c
            WHERE (:search IS NULL
                   OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR c.status = :status)
              AND (:isActive IS NULL OR c.isActive = :isActive)
            """)
    Page<Coupon> findAllWithFilters(@Param("search") String search,
                                    @Param("status") CouponStatus status,
                                    @Param("isActive") Boolean isActive,
                                    Pageable pageable);

    /**
     * Récupère les coupons expirés (date de fin dépassée)
     */
    List<Coupon> findByEndDateBefore(LocalDateTime now);

    // Date de fin dépassée : coupons encore ACTIVE, PLANNED ou DISABLED (pas déjà EXPIRED).
    List<Coupon> findByEndDateBeforeAndStatusIn(LocalDateTime now, Collection<CouponStatus> statuses);

    // Coupons encore en PLANNED alors qu’on est déjà dans la période (début passé, fin pas encore) : pour activation auto.
    @Query("""
            SELECT c FROM Coupon c
            WHERE c.status = 'PLANNED'
              AND c.startDate <= :now
              AND c.endDate >= :now
            """)
    List<Coupon> findPlannedCouponsToAutoActivate(@Param("now") LocalDateTime now);

    /**
     * Vérifie s'il existe un coupon avec le code fourni et Active
     */
    Optional<Coupon> findByCodeAndIsActiveTrue(String couponCode);

    // ============================================================================
    // 🏠 Accueil salarié
    // ============================================================================

    /**
     * Récupère le dernier coupon actif en cours de validité
     */
    @Query("""
            SELECT c
            FROM Coupon c
            WHERE c.isActive = true
              AND c.status = 'ACTIVE'
              AND c.startDate <= :now
              AND c.endDate >= :now
            ORDER BY c.startDate DESC
            """)
    Optional<Coupon> findLatestActiveCoupon(@Param("now") LocalDateTime now);

    /**
     * Liste tous les coupons actifs en cours de validité (accueil salarié, codes promo panier). n’est pas lié à un produit ou une catégorie.
     * Utilisé pour l’accueil salarié : coupons à saisir manuellement (panier, livraison, etc.).
     */
    @Query("""
            SELECT c
            FROM Coupon c
            WHERE c.isActive = true
              AND c.status = 'ACTIVE'
              AND c.startDate <= :now
              AND c.endDate >= :now
            ORDER BY c.startDate DESC
            """)
    List<Coupon> findActiveCouponsInValidity(@Param("now") LocalDateTime now);

    /** Nombre de coupons avec le statut donné. */
    long countByStatus(CouponStatus status);

    /** Nombre de coupons actifs (isActive = true). Utilisé pour le KPI dashboard commercial. */
    long countByIsActiveTrue();

    /** Somme des utilisations (usageCount) pour tous les coupons. */
    @Query("SELECT COALESCE(SUM(c.usageCount), 0) FROM Coupon c")
    long sumUsageCount();

    /** Somme du montant généré (totalGenerated) pour tous les coupons. */
    @Query("SELECT COALESCE(SUM(c.totalGenerated), 0) FROM Coupon c")
    BigDecimal sumTotalGenerated();

}

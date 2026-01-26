package com.example.coopachat.repositories;

import com.example.coopachat.entities.Coupon;
import com.example.coopachat.enums.CouponScope;
import com.example.coopachat.enums.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
     * Vérifie si un coupon existe déjà avec ce code (hors coupon courant).
     */
    boolean existsByCodeAndIdNot(String code, Long id);

    /**
     * Vérifie si un coupon existe déjà avec ce nom (hors coupon courant).
     */
    boolean existsByNameAndIdNot(String name, Long id);

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
              AND (:scope IS NULL OR c.scope = :scope)
              AND (:isActive IS NULL OR c.isActive = :isActive)
            """)
    Page<Coupon> findAllWithFilters(@Param("search") String search,
                                    @Param("status") CouponStatus status,
                                    @Param("scope") CouponScope scope,
                                    @Param("isActive") Boolean isActive,
                                    Pageable pageable);

    /**
     * Récupère les coupons expirés (date de fin dépassée)
     */
    List<Coupon> findByEndDateBefore(LocalDateTime now);

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
}

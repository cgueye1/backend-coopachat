package com.example.coopachat.repositories;

import com.example.coopachat.entities.Promotion;
import com.example.coopachat.enums.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'entité Promotion.
 */
@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /** Vérifie s'il existe au moins une promotion active (une seule autorisée à la fois). */
    boolean existsByIsActiveTrue();

    /** Liste paginée avec filtres optionnels (recherche par nom, statut). */
    @Query("""
            SELECT p FROM Promotion p
            WHERE (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR p.status = :status)
            """)
    Page<Promotion> findAllWithFilters(@Param("search") String search,
                                      @Param("status") CouponStatus status,
                                      Pageable pageable);

    /** Nombre de promotions par statut. */
    long countByStatus(CouponStatus status);
}

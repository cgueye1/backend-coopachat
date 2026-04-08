package com.example.coopachat.repositories;

import com.example.coopachat.entities.Claim;
import com.example.coopachat.enums.ClaimDecisionType;
import com.example.coopachat.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    /** Compte le nombre de réclamations créées entre deux dates (inclus). */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Liste paginée des réclamations pour le responsable logistique (gestion des retours).
     * - search : optionnel, recherche par numéro de commande, nom du client (prénom + nom) ou nom du produit (ligne concernée)
     * - status : optionnel, filtre par statut (En attente, Validé, Rejeté)
     * Tri : du plus récent au plus ancien (createdAt DESC).
     *
     */
    @Query("SELECT DISTINCT c FROM Claim c " +
            "JOIN c.order o " +
            "JOIN c.employee e " +
            "JOIN e.user u " +
            "LEFT JOIN c.orderItem oi " +
            "LEFT JOIN oi.product p " +
            "WHERE (:search IS NULL OR :search = '' OR " +
            "      LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      (p IS NOT NULL AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))) " +
            "AND (:status IS NULL OR c.status = :status) " +
            "ORDER BY c.createdAt DESC")
    Page<Claim> findAllWithFilters(@Param("search") String search,
                                    @Param("status") ClaimStatus status,
                                    Pageable pageable);

    // ============================================================================
    // Statistiques (Gestion des retours)
    // ============================================================================

    long countByStatus(ClaimStatus status);

    long countByDecisionType(ClaimDecisionType decisionType);

    /**
     * Somme des montants remboursés (toutes réclamations avec decisionType = REMBOURSEMENT).
     * - SUM(c.refundAmount) : additionne les montants des réclamations ayant un remboursement.
     * - WHERE refundAmount IS NOT NULL : on ne compte que les réclamations effectivement remboursées.
     * - COALESCE(..., 0) : si aucune réclamation remboursée, SUM renvoie null ; COALESCE remplace null par 0 pour éviter un BigDecimal null.
     */
    @Query("SELECT COALESCE(SUM(c.refundAmount), 0) FROM Claim c WHERE c.refundAmount IS NOT NULL")
    BigDecimal sumRefundAmount();

    /**
     * Liste paginée des réclamations d'un employé (historique des retours du salarié connecté).
     * Filtre optionnel par statut. Tri : du plus récent au plus ancien.
     */
    @Query("SELECT c FROM Claim c WHERE c.employee.id = :employeeId " +
            "AND (:status IS NULL OR c.status = :status) ORDER BY c.createdAt DESC")
    Page<Claim> findByEmployeeIdOrderByCreatedAtDesc(
            @Param("employeeId") Long employeeId,
            @Param("status") ClaimStatus status,
            Pageable pageable);
}

package com.example.coopachat.dtos.claim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Statistiques pour la page « Gestion des retours » : Total, Validés, Rejetés, Réintégrés, Montant remboursé.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimStatsDTO {

    /** Nombre total de réclamations (retours). */
    private long total;

    /** Nombre de réclamations validées. */
    private long validatedCount;

    /** Nombre de réclamations rejetées. */
    private long rejectedCount;

    /** Nombre de réclamations réintégrées au stock (décision = Réintégration). */
    private long reintegratedCount;

    /** Montant total remboursé (somme des refundAmount). */
    private BigDecimal totalRefundAmount;
}

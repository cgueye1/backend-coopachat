package com.example.coopachat.dtos.claim;

import com.example.coopachat.enums.ClaimDecisionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour valider une réclamation : réintégration au stock ou remboursement.
 * - Réintégration : quantité à réintégrer (optionnelle, sinon toute la quantité commandée). Le montant remboursé est calculé côté serveur (sous-total de la ligne).
 * - Remboursement : le montant est calculé côté serveur (sous-total de la ligne pour la quantité concernée) ; {@code refundAmount} du DTO est ignoré.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateClaimDTO {

    @NotNull(message = "Le type de décision est obligatoire (Réintégration au stock ou Remboursement)")
    private ClaimDecisionType decisionType;

    /** Quantité à réintégrer au stock (si decisionType = REINTEGRATION). Optionnel : si absent, on réintègre toute la quantité commandée. Sinon entre 1 et quantité commandée. */
    private Integer quantityToReintegrate;

    /** Ignoré : le montant est dérivé du sous-total de la ligne commande sur le serveur. */
    private BigDecimal refundAmount;
}

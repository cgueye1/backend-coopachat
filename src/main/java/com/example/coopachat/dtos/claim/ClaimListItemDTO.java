package com.example.coopachat.dtos.claim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour un élément de la liste des réclamations (vue responsable logistique)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimListItemDTO {

    private Long claimId;
    private String orderNumber;
    private String employeeName;
    private String productName;           // ← UN produit
    private String productImage;
    private Integer quantity;             // Qté du produit réclamé
    private String problemTypeLabel;
    private String status;
    private LocalDateTime createdAt;
    /** Décision : libellé (Réintégration au stock, Remboursement) ou null si en attente / rejeté. */
    private String decisionLabel;
    /** Montant remboursé (si décision = Remboursement). */
    private BigDecimal refundAmount;
}
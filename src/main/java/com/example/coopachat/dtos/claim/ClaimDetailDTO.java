package com.example.coopachat.dtos.claim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTO pour « voir détails » d'une réclamation. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDetailDTO {

    // Infos réclamation
    private Long claimId;
    private String orderNumber;
    private String status;
    private LocalDateTime createdAt;

    // Infos client
    private String employeeName;
    private String employeePhone;

    // Produit concerné (orderItemId pour le remboursement RL)
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantityOrdered;      // Qté commandée
    private BigDecimal subtotalProduct;

    // Détails problème
    private String problemTypeLabel;
    private String comment;
    private List<String> photoUrls;

    // Décision (si validé ou rejeté)
    private String decisionTypeLabel;  // Réintégration au stock / Remboursement
    private BigDecimal refundAmount;
    private String rejectionReason;   // Motif si rejeté
    /** Date/heure à laquelle le RL a traité la réclamation. */
    private LocalDateTime processedAt;
    /** Nom complet du RL ayant traité la réclamation. */
    private String processedByName;
}

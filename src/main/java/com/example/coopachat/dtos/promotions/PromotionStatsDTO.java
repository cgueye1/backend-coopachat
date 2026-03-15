package com.example.coopachat.dtos.promotions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistiques des promotions (produit), pour les cartes du haut comme l'interface coupons.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionStatsDTO {

    /** Nombre total de promotions. */
    private long totalPromotions;
    /** Promotions actuellement actives (isActive + dates valides). */
    private long promotionsActives;
    /** Promotions planifiées (statut PLANNED). */
    private long promotionsPlanifiees;
    /** Promotions expirées (statut EXPIRED). */
    private long promotionsExpirees;
    /** Promotions désactivées (statut DISABLED). */
    private long promotionsDesactivees;
    /** Nombre total de produits concernés (toutes promotions confondues). */
    private long totalProduitsConcernes;
}

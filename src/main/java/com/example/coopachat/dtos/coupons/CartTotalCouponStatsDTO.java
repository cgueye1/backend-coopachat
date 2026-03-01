package com.example.coopachat.dtos.coupons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Statistiques des coupons "panier" (scope CART_TOTAL uniquement).
 * Utilisé pour les cartes "Coupons actives", "Utilisations totales" et "Montant généré" de l'interface commercial.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartTotalCouponStatsDTO {

    /** Nombre de coupons actifs dont la portée est CART_TOTAL. */
    private long activeCouponsCount;

    /** Nombre total d'utilisations de ces coupons (somme des usageCount). */
    private long totalUsages;

    /** Montant total généré par l'utilisation de ces coupons (somme des totalGenerated). */
    private BigDecimal totalGenerated;
}

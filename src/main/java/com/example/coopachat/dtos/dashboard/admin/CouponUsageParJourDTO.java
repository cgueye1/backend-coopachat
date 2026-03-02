package com.example.coopachat.dtos.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un point du graphique "Tendance des coupons utilisés" (7 derniers jours).
 * Pour chaque jour : date (dd/MM), nombre de fois où un coupon a été utilisé (commandes avec coupon).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsageParJourDTO {

    /** Libellé du jour (ex. "25/02"). */
    private String date;

    /** Nombre d'utilisations de coupon ce jour (nombre de commandes avec coupon créées ce jour). */
    private long nbUtilisations;
}

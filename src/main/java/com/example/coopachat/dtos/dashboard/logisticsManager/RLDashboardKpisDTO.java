package com.example.coopachat.dtos.dashboard.logisticsManager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KPIs du tableau de bord RL (Responsable Logistique).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RLDashboardKpisDTO {

    /** Nombre de commandes en attente (status = EN_ATTENTE). */
    private long commandesEnAttente;

    /** Nombre de commandes en retard (EN_ATTENTE et deliveryDate < aujourd'hui). */
    private long commandesEnRetard;

    /** Nombre de tournées actives (status = EN_COURS). */
    private long tourneesActives;

    /** Nombre de commandes livrées ce mois (LIVREE, deliveryCompletedAt <= début du mois). */
    private long livreesCeMois;
}

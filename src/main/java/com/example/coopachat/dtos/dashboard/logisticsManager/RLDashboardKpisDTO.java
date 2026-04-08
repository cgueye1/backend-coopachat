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

    /** Commandes EN_ATTENTE, sans tournée, salarié actif (planifiable — aligné planification). */
    private long commandesEnAttente;

    /** Sous-ensemble « en retard » : même périmètre + deliveryDate &lt; aujourd'hui (aligné badge calendrier). */
    private long commandesEnRetard;

    /** Nombre de tournées actives (status = EN_COURS). */
    private long tourneesActives;

    /** Nombre de commandes livrées ce mois (LIVREE, deliveryCompletedAt <= début du mois). */
    private long livreesCeMois;
}

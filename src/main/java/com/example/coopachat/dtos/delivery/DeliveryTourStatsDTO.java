package com.example.coopachat.dtos.delivery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques des tournées par statut
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryTourStatsDTO {

    /**
     * Tournées planifiées (brouillon)
     */
    private long plannedTours;

    /**
     * Tournées assignées au livreur
     */
    private long assignedTours;

    /**
     * Tournées en cours
     */
    private long inProgressTours;

    /**
     * Tournées terminées
     */
    private long completedTours;

    /**
     * Tournées annulées
     */
    private long cancelledTours;

}
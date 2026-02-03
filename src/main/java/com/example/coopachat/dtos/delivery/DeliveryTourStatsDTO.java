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
     * Tournées planifiées
     */
    private long plannedTours;

    /**
     * Tournées proposées
     */
    private long proposedTours;

    /**
     * Tournées confirmées
     */
    private long confirmedTours;

    /**
     * Tournées terminées
     */
    private long completedTours;

    /**
     * Tournées annulées
     */
    private long cancelledTours;

}
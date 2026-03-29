package com.example.coopachat.dtos.delivery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques des tournées : total + répartition par statut (ASSIGNEE, EN_COURS, TERMINEE, ANNULEE).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryTourStatsDTO {

    /**
     * Nombre total de tournées (tous statuts confondus).
     */
    private long totalTours;

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
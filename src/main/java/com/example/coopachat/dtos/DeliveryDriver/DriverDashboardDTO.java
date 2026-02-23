package com.example.coopachat.dtos.DeliveryDriver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour le tableau de bord du livreur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDashboardDTO {

    /** Nombre de livraisons effectuées aujourd'hui par le livreur. */
    private long livraisonsAujourdhui;

    /** Nombre total de livraisons effectuées par le livreur (toutes dates). */
    private long totalLivraisons;

    /**
     * Satisfaction moyenne : somme des rating (1 à 5) ÷ nombre d'avis.
     * Null si le livreur n'a encore aucun avis (DriverReview).
     */
    private Double satisfactionMoyenne;
}

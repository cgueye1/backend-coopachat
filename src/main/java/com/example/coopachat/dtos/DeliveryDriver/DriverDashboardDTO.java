package com.example.coopachat.dtos.DeliveryDriver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour le tableau de bord du livreur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDashboardDTO {

    // Infos livreur (header)
    private String firstName;
    private String lastName;
    private String photoUrl;
    private Boolean isOnline;
    private String vehicleType;

    /** Nombre de livraisons effectuées aujourd'hui par le livreur. */
    private long livraisonsAujourdhui;

    /** Nombre total de livraisons effectuées par le livreur (toutes dates). */
    private long totalLivraisons;

    /** Gains aujourd'hui (somme des DriverEarning du jour). */
    private BigDecimal gainsAujourdhui;

    /** Tarif par livraison (ex. 500 F CFA) configuré par l'admin. */
    private BigDecimal tarifParLivraison;

    /**
     * Satisfaction moyenne : somme des rating (1 à 5) ÷ nombre d'avis.
     * Null si le livreur n'a encore aucun avis (DriverReview).
     */
    private Double satisfactionMoyenne;

    /**
     * Performances du mois en cours : tranches de 7 jours (S1, S2, …) → nombre de livraisons.
     */
    private java.util.List<DriverPerformanceItemDTO> performances;
}

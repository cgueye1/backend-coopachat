package com.example.coopachat.dtos.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un point du graphique « Livraisons 7 jours » (Admin et RL), homogène sur la date de livraison prévue.
 * Pour chaque jour affiché : effectifs basés sur {@code Order.deliveryDate} (sauf mention contraire).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivraisonParJourDTO {

    /** Libellé du jour (ex. "06/09"). */
    private String date;

    /**
     * Nombre de commandes dont la date de livraison prévue est ce jour, hors annulées
     * ({@code deliveryDate} = jour, statut ≠ ANNULEE).
     */
    private long nbPrevues;

    /**
     * Parmi le prévu ce jour, commandes effectivement livrées
     * ({@code deliveryDate} = jour et statut LIVREE).
     */
    private long nbLivreesALaDate;

    /**
     * Commandes EN_ATTENTE dont la date de livraison prévue est strictement avant ce jour
     * (non planifiées / date dépassée au sens calendrier prévu).
     */
    private long nbRetard;
}

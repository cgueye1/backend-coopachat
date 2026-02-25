package com.example.coopachat.dtos.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un point du graphique "Commandes vs Livraisons" pour un jour donné.
 * Utilisé pour les 7 derniers jours.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandesVsLivraisonsDayDTO {

    /** Libellé du jour (ex. "05/02" ou "06/09"). */
    private String date;

    /** Nombre de commandes "en attente" (EN_ATTENTE) pour ce jour */
    private long commandesEnAttente;

    /** Nombre de livraisons (LIVREE) pour ce jour. */
    private long livraisons;
}

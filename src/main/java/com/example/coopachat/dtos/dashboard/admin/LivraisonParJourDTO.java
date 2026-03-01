package com.example.coopachat.dtos.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un point du graphique "Livraisons 7 jours" (Admin et RL).
 * Pour chaque jour : date, nbLivrees, nbAssignes, nbEnAttente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivraisonParJourDTO {

    /** Libellé du jour (ex. "06/09"). */
    private String date;

    /** Nombre de commandes livrées ce jour (LIVREE, deliveryCompletedAt ce jour). */
    private long nbLivrees;

    /** Nombre de commandes assignées à une tournée ce jour (VALIDEE, EN_PREPARATION, EN_COURS, ARRIVE) avec deliveryDate = ce jour. */
    private long nbAssignes;

    /** Nombre de commandes en attente ce jour (EN_ATTENTE avec deliveryDate = ce jour, non encore en tournée). */
    private long nbEnAttente;
}

package com.example.coopachat.dtos.dashboard.logisticsManager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un point du graphique "Commandes par jour" (7 derniers jours) pour le RL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandesParJourDTO {

    /** Libellé du jour (ex. "06/09"). */
    private String date;

    /** Nombre de commandes créées ce jour-là. */
    private long nbCommandes;
}

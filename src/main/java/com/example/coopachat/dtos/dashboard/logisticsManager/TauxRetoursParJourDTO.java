package com.example.coopachat.dtos.dashboard.logisticsManager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un point du graphique « Taux de retours (%) » sur le tableau de bord responsable logistique.
 * Pour chaque jour : date (dd/MM) et taux = (nombre de réclamations créées ce jour / nombre de commandes ce jour) × 100.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TauxRetoursParJourDTO {

    /** Libellé du jour (ex. "06/09"). */
    private String date;

    /** Taux de retours en % (réclamations / commandes × 100). 0 si aucune commande ce jour. */
    private double tauxPercent;
}

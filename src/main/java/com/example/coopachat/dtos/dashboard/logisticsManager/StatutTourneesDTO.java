package com.example.coopachat.dtos.dashboard.logisticsManager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Graphique "Statut tournées" : effectif par statut (ASSIGNEE, EN_COURS, TERMINEE, ANNULEE).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatutTourneesDTO {

    /** Clé = statut (ex. "ASSIGNEE", "EN_COURS"), valeur = effectif. */
    private Map<String, Long> parStatut;
}

package com.example.coopachat.dtos.dashboard.commercial;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * KPIs du tableau de bord commercial (GET /api/commercial/dashboard/kpis).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommercialDashboardKpisDTO {

    private long totalSalaries;
    private long nouveauxSalariesCeMois;
    private long commandesCeMois;
    private Double evolutionCommandesPct;
    private BigDecimal ventesCeMois;
    private Double evolutionVentesPct;
    private long promotionsActives;

    /** Graphique 1 — Évolution des ventes (6 derniers mois). */
    private List<VentesParMoisDTO> evolutionVentes;
    /** Graphique 2 — Nombre de commandes par mois (6 derniers mois). */
    private List<CommandesParMoisDTO> evolutionCommandes;
}

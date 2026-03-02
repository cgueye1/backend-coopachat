package com.example.coopachat.dtos.dashboard.commercial;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Un point du graphique "Évolution des ventes" (mois + montant). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentesParMoisDTO {
    /** Libellé du mois (ex. "Jan", "Fév"). */
    private String mois;
    /** Montant des ventes (commandes LIVREE) ce mois. */
    private BigDecimal montant;
}

package com.example.coopachat.dtos.dashboard.commercial;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un point du graphique "Nombre de commandes" (mois + nombre). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandesParMoisDTO {
    /** Libellé du mois (ex. "Jan", "Fév"). */
    private String mois;
    /** Nombre de commandes créées ce mois. */
    private long nbCommandes;
}

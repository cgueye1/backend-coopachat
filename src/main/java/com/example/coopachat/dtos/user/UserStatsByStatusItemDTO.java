package com.example.coopachat.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un élément du graphique "Répartition des statuts"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsByStatusItemDTO {

    //Libellé affichable : "Actifs" ou "Inactifs".
    private String label;

    // Nombre d'utilisateurs (actifs ou inactifs).
    private long count;

    // Part en % du total (0–100).
    private double percentage;
}

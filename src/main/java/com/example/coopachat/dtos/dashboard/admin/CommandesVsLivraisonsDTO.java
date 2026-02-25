package com.example.coopachat.dtos.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour le graphique "Commandes vs Livraisons (7 derniers jours)".
 * Une série = commandes en attente, l’autre = livraisons (LIVREE).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandesVsLivraisonsDTO {

    /** Les 7 derniers jours, avec pour chaque jour : date, commandesEnAttente, livraisons. */
    private List<CommandesVsLivraisonsDayDTO> derniersJours;
}

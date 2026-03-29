package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistiques pour la page Gestion des commandes (RL) : total (hors annulées), EN ATTENTE, EN RETARD, EN COURS, VALIDÉES, LIVRÉES ce mois.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeOrderStatsDTO {

    /** Nombre total de commandes (tous statuts sauf ANNULEE). */
    private long totalCommandes;

    /** Commandes en attente (non affectées à une tournée) — À valider par le RL. */
    private long enAttente;

    /** Commandes dont la date de livraison souhaitée est dépassée — date dépassée. */
    private long enRetard;

    /** Tournées en cours (statut EN_COURS) — tournées actives. */
    private long enCours;

    /** Commandes validées (statut VALIDEE, affectées à une tournée). */
    private long validees;

    /** Commandes livrées ce mois (statut LIVREE, deliveryCompletedAt dans le mois). */
    private long livreesCeMois;
}

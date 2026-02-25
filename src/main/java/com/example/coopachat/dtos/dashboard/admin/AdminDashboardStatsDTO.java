package com.example.coopachat.dtos.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour GET /api/admin/dashboard/stats?periode=TODAY|THIS_MONTH.
 * 3 KPIs (indicateur clé de performance) + répartition des paiements par statut.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsDTO {

    /** KPI : nombre de commandes en attente (sur la période). */
    private long commandesEnAttente;

    /** KPI : nombre de paiements échoués (sur la période). */
    private long paiementsEchoues;

    /** KPI : nombre de réclamations ouvertes (en attente). */
    private long reclamationsOuvertes;

    /** Paiements par statut (suit le filtre période). Ex. Payé: 10, En attente: 2, Échoué: 1. */
    private List<PaymentStatusItemDTO> paiementsParStatut;
}

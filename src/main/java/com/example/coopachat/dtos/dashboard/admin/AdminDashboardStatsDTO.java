package com.example.coopachat.dtos.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour GET /api/admin/dashboard/stats (données globales, sans filtre de période).
 * 3 KPIs + répartition des paiements par statut.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsDTO {

    /** KPI : nombre de commandes en attente. */
    private long commandesEnAttente;

    /** KPI : nombre de paiements échoués. */
    private long paiementsEchoues;

    /** KPI : nombre de réclamations ouvertes (en attente). */
    private long reclamationsOuvertes;

    /** Paiements par statut (Payé, En attente, Échoué). */
    private List<PaymentStatusItemDTO> paiementsParStatut;
}

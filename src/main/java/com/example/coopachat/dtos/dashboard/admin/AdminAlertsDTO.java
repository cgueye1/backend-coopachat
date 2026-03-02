package com.example.coopachat.dtos.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Réponse de GET /api/admin/alerts.
 * Liste des alertes à afficher sur le tableau de bord admin (livraisons en retard, stocks sous seuil, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAlertsDTO {

    /** Liste des alertes (chaque alerte contient type, message, detail, module, date). */
    private List<AlertItemDTO> alerts;
}

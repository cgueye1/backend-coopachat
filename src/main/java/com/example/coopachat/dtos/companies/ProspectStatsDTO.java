package com.example.coopachat.dtos.companies;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques de prospection d'un commercial.
 * Total prospects + comptage par statut de prospection.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProspectStatsDTO {
    private long total;
    private long enAttente;      // PENDING
    private long interesses;     // INTERESTED
    private long relancer;       // RELAUNCHED
    private long signes;         // PARTNER_SIGNED
}

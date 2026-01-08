package com.example.coopachat.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques des entreprises d'un commercial
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyStatsDTO {

    /**
     * Nombre total d'entreprises créées par le commercial
     */
    private long totalCompanies;

    /**
     * Nombre d'entreprises actives (isActive = true)
     */
    private long activeCompanies;

    /**
     * Nombre d'entreprises inactives (isActive = false)
     */
    private long inactiveCompanies;
}



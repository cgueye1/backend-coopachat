package com.example.coopachat.dtos.employees;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques des employés d'un commercial
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeStatsDTO {

    /**
     * Nombre total d'employés créés par le commercial
     */
    private long totalEmployees;

    /**
     * Nombre d'employés actifs (isActive = true)
     */
    private long activeEmployees;

    /**
     * Nombre d'employés en attente d'activation (isActive = false)
     */
    private long pendingEmployees;
}


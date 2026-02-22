package com.example.coopachat.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques de la page Gestion des utilisateurs (total, actifs, inactifs).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {

    /** Nombre total d'utilisateurs */
    private long totalUsers;

    /** Nombre d'utilisateurs actifs */
    private long activeUsers;

    /** Nombre d'utilisateurs inactifs */
    private long inactiveUsers;
}

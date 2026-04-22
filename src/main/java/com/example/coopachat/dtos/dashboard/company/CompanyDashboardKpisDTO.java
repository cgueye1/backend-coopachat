package com.example.coopachat.dtos.dashboard.company;

import com.example.coopachat.dtos.dashboard.commercial.CommandesParMoisDTO;
import lombok.Data;
import java.util.List;

@Data
public class CompanyDashboardKpisDTO {
    private long totalEmployees;
    private long activeEmployees;
    private long inactiveEmployees;
    private long ordersThisMonth;
    
    // Pour l'affichage formaté (ex: "9/22")
    private String activeEmployeesRatio;

    // Pour le graphique "Commandes par mois"
    private List<CommandesParMoisDTO> evolutionCommandes;

    // Pour le top 5 des employés
    private List<TopEmployeeDTO> topEmployees;
}

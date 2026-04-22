package com.example.coopachat.dtos.dashboard.company;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopEmployeeDTO {
    private String firstName;
    private String lastName;
    private String employeeCode;
    private long nbCommandes;
    private String status; // Actif, Inactif
    private int activite; // Pourcentage (optionnel, on peut le calculer ou mettre une valeur fixe)
}

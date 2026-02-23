package com.example.coopachat.dtos.employees;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la liste des employés
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeListItemDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String companyName;
    
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;
    
    private String status; // État actif/inactif de l'employé
    private String employeeCode; // Code unique de l'employé
}


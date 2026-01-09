package com.example.coopachat.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les détails complets d'un employé
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDetailsDTO {

    private Long id;
    private String employeeCode; // Code unique de l'employé
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String companyName; // Nom de l'entreprise
    private Long companyId; // ID de l'entreprise pour le frondEnd
    
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt; // Date d'inscription
    
    private String status; // État actif/inactif de l'employé
}


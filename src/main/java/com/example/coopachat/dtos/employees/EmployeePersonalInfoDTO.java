package com.example.coopachat.dtos.employees;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'affichage et la modification des infos personnelles
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePersonalInfoDTO {
    private Long id;
    private String firstName;      // Modifiable
    private String lastName;       // Modifiable
    private String phone;          // Modifiable
    private String email;          // Lecture seule (affiché mais ignoré à l'update)
    private String companyName;    // Lecture seule (affiché mais ignoré à l'update)
}
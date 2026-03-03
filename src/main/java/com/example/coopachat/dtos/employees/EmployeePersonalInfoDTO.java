package com.example.coopachat.dtos.employees;

import lombok.*;

/**
 * DTO pour l'affichage et la modification des infos personnelles
 */
@Getter
@AllArgsConstructor
public class EmployeePersonalInfoDTO {

    @Setter
    private String firstName;      // Modifiable
    @Setter
    private String lastName;       // Modifiable
    @Setter
    private String phone;          // Modifiable

    private String photoprofil;    //photoProfile

    private String email;          // Lecture seule (affiché mais ignoré à l'update)
    private String companyName;    // Lecture seule (affiché mais ignoré à l'update)
}
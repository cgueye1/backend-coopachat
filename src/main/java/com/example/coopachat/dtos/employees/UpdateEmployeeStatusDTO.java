package com.example.coopachat.dtos.employees;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la modification du statut actif/inactif d'un employé
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEmployeeStatusDTO {

    @NotNull(message = "Le statut actif/inactif est obligatoire")
    private Boolean isActive; // true pour activer, false pour désactiver
}





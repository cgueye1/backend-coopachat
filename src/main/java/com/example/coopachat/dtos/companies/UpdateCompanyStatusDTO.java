package com.example.coopachat.dtos.companies;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la modification du statut actif/inactif d'une entreprise
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCompanyStatusDTO {

    @NotNull(message = "Le statut actif/inactif est obligatoire")
    private Boolean isActive;
}



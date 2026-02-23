package com.example.coopachat.dtos.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour activer / désactiver un utilisateur (toggle statut).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusDTO {

    /** true = Actif, false = Inactif */
    @NotNull(message = "Le statut (isActive) est obligatoire")
    private Boolean isActive;
}

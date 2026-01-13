package com.example.coopachat.dtos.products;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la modification du statut d'un produit (actif/inactif)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductStatusDTO {

    @NotNull(message = "Le statut actif/inactif est obligatoire")
    private Boolean status; // Statut actif/inactif (true = actif, false = inactif)
}


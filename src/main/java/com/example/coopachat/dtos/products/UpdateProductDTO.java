package com.example.coopachat.dtos.products;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour la modification d'un produit existant
 * Tous les champs sont optionnels - seuls les champs fournis seront mis à jour
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductDTO {


    private String name; // Nom du produit (optionnel)

    private String description; // Description du produit (optionnel)

    private Long categoryId; // ID de la catégorie (optionnel)

    @Positive(message = "Le prix doit être positif")
    private BigDecimal price; // Prix unitaire (optionnel)

    @PositiveOrZero(message = "Le seuil minimum ne peut pas être négatif")
    private Integer minThreshold; // Seuil minimum de réapprovisionnement (optionnel)

    @PositiveOrZero(message = "Le stock ne peut pas être négatif")
    private Integer currentStock; // Stock actuel (optionnel)

    private String image; // Image du produit (URL/fichier) (optionnel)
}


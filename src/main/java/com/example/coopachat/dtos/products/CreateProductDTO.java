package com.example.coopachat.dtos.products;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour la création d'un produit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductDTO {

    @NotBlank(message = "Le nom du produit est obligatoire")
    private String name; // Nom du produit

    @NotNull(message = "La catégorie est obligatoire")
    private Long categoryId; // ID de la catégorie

    @NotNull(message = "Le prix unitaire est obligatoire")
    @Positive(message = "Le prix doit être positif")
    private BigDecimal price; // Prix unitaire

    @NotNull(message = "Le stock initial est obligatoire")
    @PositiveOrZero(message = "Le stock initial ne peut pas être négatif")
    private Integer currentStock; // Stock initial (quantité disponible)

    private String description; // Description du produit (optionnel)

    private String image; // Image du produit (URL/fichier) (optionnel)

    private Boolean status = false; // Statut actif/inactif
    private Integer minThreshold = 0; // Seuil minimum de réapprovisionnement (optionnel, défaut: 0)
}




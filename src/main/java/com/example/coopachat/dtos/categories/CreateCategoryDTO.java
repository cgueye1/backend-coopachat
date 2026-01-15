package com.example.coopachat.dtos.categories;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la création d'une catégorie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryDTO {

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    private String name; // Nom de la catégorie (ex: "Épicerie", "Boissons", "Frais", "Hygiène")
}



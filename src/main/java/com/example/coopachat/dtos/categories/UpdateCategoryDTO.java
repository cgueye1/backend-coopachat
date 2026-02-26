package com.example.coopachat.dtos.categories;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour modifier une catégorie. Seuls les champs non null sont mis à jour.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryDTO {

    /** Nouveau nom (optionnel). */
    private String name;

    /** Nouvelle icône, ex. nom d'icône ou URL (optionnel). */
    private String icon;
}

package com.example.coopachat.dtos.categories;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour afficher une catégorie dans l'accueil.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryHomeItemDTO {

    private Long id;
    private String name;
    private String icon;
}

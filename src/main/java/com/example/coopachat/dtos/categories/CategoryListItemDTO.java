package com.example.coopachat.dtos.categories;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour afficher une catégorie dans une liste
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryListItemDTO {

    private Long id;
    private String name;
}


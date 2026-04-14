package com.example.coopachat.dtos.categories;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour afficher une catégorie dans une liste (icon + nom).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryListItemDTO {

    private Long id;
    private String name;
    /** Icône (nom ou URL), peut être null. */
    private String icon;
    /** Nombre total de produits rattachés à la catégorie. */
    private long productCount;
    /** Nombre de produits actifs rattachés à la catégorie. */
    private long activeProductCount;
}


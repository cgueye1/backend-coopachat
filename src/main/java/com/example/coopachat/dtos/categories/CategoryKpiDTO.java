package com.example.coopachat.dtos.categories;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les KPI de la page catégories (en un seul appel).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryKpiDTO {

    /**
     * Nombre total de catégories.
     */
    private long totalCategories;

    /**
     * Nombre total de produits.
     */
    private long totalProducts;

    /**
     * Nombre de produits actifs.
     */
    private long activeProducts;
}

package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques du catalogue produits
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductStatsDTO {

    /**
     * Nombre total de produits
     */
    private long totalProducts;

    /**
     * Nombre de produits actifs (status = true)
     */
    private long activeProducts;

    /**
     * Nombre de produits inactifs (status = false)
     */
    private long inactiveProducts;
}




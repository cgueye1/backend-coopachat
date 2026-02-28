package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques du suivi des stocks
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockStatsDTO {

    private long total; // Nombre total de produits suivis
    private long lowStock; // Produits sous le seuil (stock > 0 et < seuil)
    private long outOfStock; // Produits en rupture (stock = 0)
    private long sufficient; // Produits avec stock suffisant (stock >= seuil)
}




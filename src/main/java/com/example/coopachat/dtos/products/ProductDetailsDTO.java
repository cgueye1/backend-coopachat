package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour afficher les détails d'un produit
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailsDTO {

    private String productCode; // Code unique du produit
    private String image; // Image du produit
    private String name; // Nom du produit
    private String description; // Description complète
    private String categoryName; // Nom de la catégorie
    private BigDecimal price; // Prix unitaire
    private Boolean status; // Statut actif/inactif (true = actif, false = inactif)
    private String currentStockStatus; //statut (En Stock/Rupture)
}





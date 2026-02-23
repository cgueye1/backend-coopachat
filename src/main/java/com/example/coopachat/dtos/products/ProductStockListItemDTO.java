package com.example.coopachat.dtos.products;

import com.example.coopachat.enums.EtatStock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour un élément de la liste de suivi des stocks
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductStockListItemDTO {

    private Long id; // ID du produit
    private String name; // Nom du produit
    private String productCode; // Référence du produit
    private String categoryName; // Nom de la catégorie
    private String image; // Image du produit (URL/fichier)
    private Integer currentStock; // Stock actuel
    private Integer minThreshold; // Seuil minimum
    private EtatStock stockStatus; // État du stock (suffisant / sous seuil / rupture)
}




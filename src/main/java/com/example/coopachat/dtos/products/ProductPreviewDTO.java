package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour avoir un aperçu du produit (sérialisé en JSON pour le front). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPreviewDTO {

    private String image; // Image du produit
    private String name; // Nom du produit
    private String categoryName; // Nom de la catégorie
    private Integer currentStock; // Stock actuel (optionnel en contexte commande)
    /** Quantité commandée pour ce produit (contexte détail commande). */
    private Integer quantity;
}

package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;

//DTO pour avoir un aperçu du produit
@AllArgsConstructor
public class ProductPreviewDTO {

    private String image; // Image du produit
    private String name; // Nom du produit
    private String categoryName; // Nom de la catégorie
    private Integer currentStock;//le stock actuel
}

package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ProductPreviewDTO {

    private String image; // Image du produit
    private String name; // Nom du produit
    private String categoryName; // Nom de la catégorie
    private Integer currentStock;//le stock actuel
}

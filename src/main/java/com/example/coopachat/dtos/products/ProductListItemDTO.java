package com.example.coopachat.dtos.products;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour afficher un produit dans la liste
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductListItemDTO {

    private Long id;
    private String name; // Nom du produit
    private String productCode; // Code unique du produit
    private String categoryName; // Nom de la catégorie
    private BigDecimal price; // Prix unitaire
    private Integer currentStock; // Stock actuel
    private String image; // Image du produit (URL/fichier)

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime updatedAt; // Date de modification

    private String status; // Statut actif/inactif
}


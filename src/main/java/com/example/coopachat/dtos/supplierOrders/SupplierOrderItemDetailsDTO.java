package com.example.coopachat.dtos.supplierOrders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les détails d'un produit dans une commande fournisseur (item)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOrderItemDetailsDTO {

    private String productName; // Nom du produit

    private String productCategory; // Nom de la catégorie du produit

    private String productImage; // Image du produit (URL/fichier)

    private Integer quantityOrdered; // Quantité commandée
}



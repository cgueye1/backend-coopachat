package com.example.coopachat.dtos.DeliveryDriver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Une ligne produit dans le détail de commande vu par le livreur (Produits commandés).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemForDriverDTO {

    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    /** URL ou chemin de l'image du produit (optionnel). */
    private String imageUrl;
}

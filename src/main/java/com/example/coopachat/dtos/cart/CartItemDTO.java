package com.example.coopachat.dtos.cart;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Long id;               // ID du CartItem
    private Long productId;        // ID du produit
    private String productName;
    private String categoryName;
    private String imageUrl;
    private Integer quantity;

    // Prix unitaire
    private BigDecimal unitPrice;  // 2591.00

    // Promo (optionnel)
    private BigDecimal promoPrice; // null si pas de promo
    private Boolean hasPromo;      // true/false

    // Sous-total (calculé : prix × quantité)
    private BigDecimal subtotal;   //
}
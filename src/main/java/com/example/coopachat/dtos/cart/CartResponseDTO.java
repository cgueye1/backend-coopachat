package com.example.coopachat.dtos.cart;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponseDTO {
    private List<CartItemDTO> items;      // Tous les articles du panier
    private BigDecimal totalPrice;        // Somme des sous-totaux
}
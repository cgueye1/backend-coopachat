package com.example.coopachat.dtos.promotions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Un produit dans une promotion avec sa réduction (%).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionProductItemDTO {

    private Long productId;
    private String productName;
    private BigDecimal discountValue; // pourcentage
}

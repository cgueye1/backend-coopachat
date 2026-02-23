package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour afficher un produit avec prix promo.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPromoItemDTO {

    private Long id;
    private String name;
    private String image;
    private String categoryName;
    private BigDecimal originalPrice;
    private BigDecimal promoPrice;
    private Integer discountPercent;
    private boolean hasPromo;
}

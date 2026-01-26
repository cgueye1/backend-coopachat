package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour afficher un produit dans le catalogue (sans catégorie).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductCatalogueItemDTO {

    private Long id;
    private String name;
    private String image;
    private BigDecimal originalPrice;
    private BigDecimal promoPrice;
    private Integer discountPercent;
    private boolean hasPromo;
}

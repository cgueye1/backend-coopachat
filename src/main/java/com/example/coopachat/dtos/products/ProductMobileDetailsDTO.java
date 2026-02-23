package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les détails produit côté mobile (sans champ "marche").
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductMobileDetailsDTO {

    private Long id;
    private String productCode;
    private String name;
    private String description;
    private String image;
    private String brand;
    private String categoryName;
    private BigDecimal originalPrice;
    private BigDecimal promoPrice;
    private Integer discountPercent;
    private boolean hasPromo;
    private Integer currentStock;
    private Boolean status;
}

package com.example.coopachat.dtos.coupons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour afficher un produit lié à un coupon
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponProductItemDTO {

    private Long id;
    private String name;
    private String categoryName;
    private String description;
    private Integer currentStock;
    private String image;
}

package com.example.coopachat.dtos.promotions;

import com.example.coopachat.enums.CouponStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour un élément de la liste des promotions (produit).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionListItemDTO {

    private Long id;
    private String name;
    private CouponStatus status;
    private Boolean isActive;
    private String startDate;   // dd-MM-yyyy
    private String endDate;    // dd-MM-yyyy
    private int productCount;  // nombre de produits dans la promotion
}

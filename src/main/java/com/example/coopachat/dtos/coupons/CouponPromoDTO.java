package com.example.coopachat.dtos.coupons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour la promo affichée sur l'accueil.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CouponPromoDTO {

    private Long id;
    private String code;
    private String name;
    private BigDecimal value;
    private String validFrom; // dd-MM-yyyy
    private String validTo;   // dd-MM-yyyy
}

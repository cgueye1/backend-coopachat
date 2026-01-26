package com.example.coopachat.dtos.home;

import com.example.coopachat.dtos.categories.CategoryHomeItemDTO;
import com.example.coopachat.dtos.coupons.CouponPromoDTO;
import com.example.coopachat.dtos.products.ProductPromoItemDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour l'accueil salarié (home).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeResponseDTO {

    private String firstName;
    private List<ProductPromoItemDTO> products; // max 4
    private List<CategoryHomeItemDTO> categories; // max 4
    private CouponPromoDTO activeCoupon; // null si aucune promo
}

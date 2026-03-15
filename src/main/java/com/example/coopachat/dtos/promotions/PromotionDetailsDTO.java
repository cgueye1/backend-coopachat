package com.example.coopachat.dtos.promotions;

import com.example.coopachat.enums.CouponStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour les détails d'une promotion (nom, dates, statut, liste des produits avec réduction).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDetailsDTO {

    private Long id;
    private String name;
    private CouponStatus status;
    private Boolean isActive;
    private String startDate;
    private String endDate;
    private List<PromotionProductItemDTO> products; // produit + % réduction
}

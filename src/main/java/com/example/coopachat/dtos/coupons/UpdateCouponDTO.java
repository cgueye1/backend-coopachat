package com.example.coopachat.dtos.coupons;

import com.example.coopachat.enums.CouponScope;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour la modification partielle d'un coupon
 * On modifie uniquement les champs non nuls.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCouponDTO {

    @Size(min = 1, message = "Le code du coupon ne peut pas être vide")
    private String code;

    @Size(min = 1, message = "Le nom du coupon ne peut pas être vide")
    private String name;

    @Positive(message = "La valeur de réduction doit être positive")
    private BigDecimal value;

    private CouponScope scope;

    private Boolean isActive;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime endDate;

    // optionnel: liste des produits pour le scope PRODUCTS
    private List<Long> productIds;

    // optionnel: liste des categories pour le scope CATEGORIES
    private List<Long> categoryIds;
}

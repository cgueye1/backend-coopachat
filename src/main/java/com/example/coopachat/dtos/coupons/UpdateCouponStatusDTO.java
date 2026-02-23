package com.example.coopachat.dtos.coupons;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour activer/désactiver un coupon
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCouponStatusDTO {

    @NotNull(message = "Le statut d'activation est obligatoire")
    private Boolean isActive;
}

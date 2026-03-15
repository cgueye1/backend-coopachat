package com.example.coopachat.dtos.coupons;

import com.example.coopachat.enums.CouponStatus;
import com.example.coopachat.enums.DiscountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour la création d'un coupon (code promo panier, réduction sur le total).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponDTO {

    @NotBlank(message = "Le code du coupon est obligatoire")
    private String code;

    @NotBlank(message = "Le nom du coupon est obligatoire")
    private String name;

    @NotNull(message = "Le type de réduction est obligatoire")
    private DiscountType discountType; // pourcentage ou montant fixe

    @NotNull(message = "La valeur de reduction est obligatoire")
    @Positive(message = "La valeur de reduction doit être positive")
    private BigDecimal value;

    @NotNull(message = "Le statut du coupon est obligatoire")
    private CouponStatus status;

    // Activation manuelle (false par défaut)
    private Boolean isActive;

    @NotNull(message = "La date de debut est obligatoire")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime startDate;

    @NotNull(message = "La date de fin est obligatoire")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime endDate;
}

package com.example.coopachat.dtos.coupons;

import com.example.coopachat.enums.CouponScope;
import com.example.coopachat.enums.CouponStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour la creation d'un coupon
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponDTO {

    @NotBlank(message = "Le code du coupon est obligatoire")
    private String code;

    @NotBlank(message = "Le nom du coupon est obligatoire")
    private String name;

    @NotNull(message = "La valeur de reduction est obligatoire")
    @Positive(message = "La valeur de reduction doit être positive")
    private BigDecimal value;

    @NotNull(message = "Le scope du coupon est obligatoire")
    private CouponScope scope;

    @NotNull(message = "Le statut du coupon est obligatoire")
    private CouponStatus status;

    // Activation manuelle (false par defaut)
    private Boolean isActive;

    @NotNull(message = "La date de debut est obligatoire")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime startDate;

    @NotNull(message = "La date de fin est obligatoire")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime endDate;

    // optionnel: liste des produits pour le scope PRODUCTS
    private List<Long> productIds;

    // optionnel: liste des categories pour le scope CATEGORIES
    private List<Long> categoryIds;
}

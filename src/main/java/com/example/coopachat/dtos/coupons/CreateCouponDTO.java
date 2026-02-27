package com.example.coopachat.dtos.coupons;

import com.example.coopachat.enums.CouponScope;
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
import java.util.List;

/**
 * DTO pour la création d'un coupon.
 * Deux types : (1) produit/catégorie (scope ALL_PRODUCTS, CATEGORIES ou PRODUCTS) ; (2) code promo panier (scope CART_TOTAL), pas lié à un produit/catégorie.
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

    @NotNull(message = "Le scope du coupon est obligatoire")
    private CouponScope scope; // ALL_PRODUCTS, CATEGORIES, PRODUCTS ou CART_TOTAL (code promo sans produit/catégorie)

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

    // Optionnel : liste des produits pour le scope PRODUCTS. Pour CART_TOTAL, doit être null ou vide.
    private List<Long> productIds;

    // Optionnel : liste des catégories pour le scope CATEGORIES. Pour CART_TOTAL, doit être null ou vide.
    private List<Long> categoryIds;


}

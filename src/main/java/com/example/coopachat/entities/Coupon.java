package com.example.coopachat.entities;

import com.example.coopachat.enums.CouponScope;
import com.example.coopachat.enums.CouponStatus;
import com.example.coopachat.enums.DiscountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity représentant un coupon de réduction ou une promotion
 */
@Entity
@Table(name = "coupons")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Le code du coupon est obligatoire")
    private String code; // Code unique du coupon/Promotion

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Le nom du coupon est obligatoire")
    private String name; // Nom unique du coupon/Promotion

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType; // type de réduction


    @Column(nullable = false)
    @NotNull(message = "La valeur de réduction est obligatoire")
    @Positive(message = "La valeur de réduction doit être positive")
    private BigDecimal value; // Pourcentage ou montant

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "Le scope du coupon est obligatoire")
    private CouponScope scope; // ALL_PRODUCTS, CATEGORIES, PRODUCTS (lié produit/catégorie) ou CART_TOTAL (code promo manuel, pas lié, s'applique au total du panier)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Le statut du coupon est obligatoire")
    private CouponStatus status; // PLANNED / ACTIVE / EXPIRED / DISABLED

    @Column(nullable = false)
    private Boolean isActive = false; // Activation manuelle

    @Column(nullable = false)
    @NotNull(message = "La date de début est obligatoire")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime startDate;

    @Column(nullable = false)
    @NotNull(message = "La date de fin est obligatoire")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime endDate;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private BigDecimal totalGenerated; // Montant total généré par le coupon

    @Column(nullable = true)
    private Integer usageCount; // Nombre d'utilisations du coupon

}

package com.example.coopachat.entities;

import com.example.coopachat.enums.DiscountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lien promotion ↔ produit avec la réduction appliquée à ce produit.
 * Une ligne = un produit dans la promotion avec sa valeur de réduction .
 */
@Entity
@Table(name = "promotion_products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromotionProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "promotion_id", nullable = false)
    @NotNull(message = "La promotion est obligatoire")
    private Promotion promotion;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Le produit est obligatoire")
    private Product product;

    /** Valeur de la réduction en pourcentage (%). */
    @Column(nullable = false)
    @NotNull(message = "La réduction est obligatoire")
    @Positive(message = "La réduction doit être positive")
    @Range(min = 0, max = 100, message = "La réduction doit être entre 0 et 100")
    private BigDecimal discountValue;
}

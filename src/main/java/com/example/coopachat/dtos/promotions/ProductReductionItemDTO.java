package com.example.coopachat.dtos.promotions;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Un produit avec sa réduction (en %) pour une promotion.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReductionItemDTO {

    @NotNull(message = "L'identifiant du produit est obligatoire")
    private Long productId;

    @NotNull(message = "La réduction est obligatoire")
    @DecimalMin(value = "0.01", message = "La réduction doit être entre 1 et 100 %")
    @DecimalMax(value = "100", message = "La réduction doit être entre 1 et 100 %")
    private BigDecimal discountValue; // en %
}

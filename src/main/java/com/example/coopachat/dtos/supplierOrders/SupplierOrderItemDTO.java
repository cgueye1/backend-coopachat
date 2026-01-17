package com.example.coopachat.dtos.supplierOrders;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour un produit dans une commande fournisseur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOrderItemDTO {

    @NotNull(message = "Le produit est obligatoire")
    private Long productId; // ID du produit

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    private Integer quantity; // Quantité commandée
}





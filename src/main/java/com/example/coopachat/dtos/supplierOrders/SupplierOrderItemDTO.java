package com.example.coopachat.dtos.supplierOrders;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour un produit dans une commande fournisseur
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOrderItemDTO {

    @NotNull(message = "Le produit est obligatoire")
    private Long productId; // ID du produit

    @JsonProperty("quantite")
    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    private Integer quantite; // Quantité commandée

}












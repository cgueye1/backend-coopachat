package com.example.coopachat.dtos.supplierOrders;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour la création d'une commande fournisseur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupplierOrderDTO {

    @NotNull(message = "Le fournisseur est obligatoire")
    private Long supplierId; // ID du fournisseur (dropdown)

    @NotEmpty(message = "Au moins un produit doit être commandé")
    @Valid //Spring valide chaque SupplierOrderItemDTO de la liste selon ses annotations (@NotNull, @Positive, etc.).
    private List<SupplierOrderItemDTO> items; // Liste des produits à commander

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime expectedDate; // Date prévue de livraison (ETA) - optionnel

    private String notes; // Note optionnelle
}


package com.example.coopachat.dtos.supplierOrders;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour la modification d'une commande fournisseur
 * Tous les champs sont optionnels - seuls les champs fournis seront mis à jour
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupplierOrderDTO {

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime expectedDate; // Date prévue de livraison (ETA) - optionnel

    private String notes; // Note optionnelle

    @Valid
    private List<SupplierOrderItemDTO> items; // Liste des produits (optionnel - si fourni, remplace toute la liste)
}




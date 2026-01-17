package com.example.coopachat.dtos.supplierOrders;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour les détails complets d'une commande fournisseur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOrderDetailsDTO {

    private Long id; // ID de la commande

    private String orderNumber; // Numéro de commande

    private String supplierName; // Nom du fournisseur

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime expectedDate; // Date prévue de livraison

    private String status; // Statut de la commande (label de l'enum)

    private String notes; // Note optionnelle

    private List<SupplierOrderItemDetailsDTO> items; // Liste des produits commandés
}





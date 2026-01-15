package com.example.coopachat.dtos.supplierOrders;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour un élément de la liste des commandes fournisseurs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOrderListItemDTO {

    private Long id; // ID de la commande

    private String orderNumber; // Numéro de commande

    private String supplierName; // Nom du fournisseur

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime expectedDate; // Date prévue de livraison

    private String productsSummary; // Résumé des produits avec quantités (ex: "Riz (100), Huile (50)")

    private String status; // Statut de la commande (label de l'enum)
}



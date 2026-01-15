package com.example.coopachat.dtos.supplierOrders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques des commandes fournisseurs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOrderStatsDTO {

    private long total; // Nombre total de commandes

    private long pending; // Commandes en attente

    private long delivered; // Commandes livrées

    private long cancelled; // Commandes annulées
}


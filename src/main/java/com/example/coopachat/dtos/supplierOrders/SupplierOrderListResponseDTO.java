package com.example.coopachat.dtos.supplierOrders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de réponse pour la liste paginée des commandes fournisseurs
 * Contient la liste des commandes de la page actuelle et les métadonnées de pagination
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOrderListResponseDTO {

    /**
     * Liste des commandes fournisseurs de la page actuelle
     */
    private List<SupplierOrderListItemDTO> content;

    /**
     * Nombre total de commandes (toutes pages confondues)
     */
    private long totalElements;

    /**
     * Nombre total de pages
     */
    private int totalPages;

    /**
     * Numéro de la page actuelle (0-indexed)
     */
    private int currentPage;

    /**
     * Taille de la page (nombre d'éléments par page)
     */
    private int pageSize;

    /**
     * Indique s'il y a une page suivante
     */
    private boolean hasNext;

    /**
     * Indique s'il y a une page précédente
     */
    private boolean hasPrevious;
}



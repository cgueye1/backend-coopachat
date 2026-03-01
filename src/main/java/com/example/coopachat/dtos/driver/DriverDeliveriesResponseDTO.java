package com.example.coopachat.dtos.driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de réponse pour la liste des livraisons du livreur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDeliveriesResponseDTO {

    /**
     * Liste des livraisons
     */
    private List<DriverDeliveryCardDTO> deliveries;

    /**
     * Nombre total d'éléments
     */
    private long totalElements;

    /**
     * Nombre total de pages
     */
    private int totalPages;

    /**
     * Page actuelle (0-based)
     */
    private int currentPage;

    /**
     * Taille de la page
     */
    private int pageSize;

    /**
     * Y a-t-il une page suivante ?
     */
    private boolean hasNext;

    /**
     * Y a-t-il une page précédente ?
     */
    private boolean hasPrevious;
}

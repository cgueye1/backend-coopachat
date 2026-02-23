package com.example.coopachat.dtos.employees;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de réponse pour la liste paginée des employés
 * Contient la liste des employés de la page actuelle et les métadonnées de pagination
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeListResponseDTO {
    
    /**
     * Liste des employés de la page actuelle
     */
    private List<EmployeeListItemDTO> content;
    
    /**
     * Nombre total d'employés (toutes pages confondues)
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


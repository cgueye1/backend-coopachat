package com.example.coopachat.dtos.claim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de réponse pour la liste paginée des réclamations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimListResponseDTO {

    private List<ClaimListItemDTO> content;      // Liste des réclamations de la page
    private long totalElements;                   // Total d'éléments
    private int totalPages;                        // Nombre total de pages
    private int currentPage;                       // Page actuelle (0-indexed)
    private int pageSize;                           // Taille de la page
    private boolean hasNext;                        // Page suivante ?
    private boolean hasPrevious;                    // Page précédente ?
}
package com.example.coopachat.dtos.coupons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour la réponse paginée de la liste des coupons
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CouponListResponseDTO {

    private List<CouponListItemDTO> content; // Liste des coupons de la page
    private long totalElements; // Nombre total de coupons
    private int totalPages; // Nombre total de pages
    private int currentPage; // Page actuelle (0-indexed)
    private int pageSize; // Taille de la page
    private boolean hasNext; // Y a-t-il une page suivante ?
    private boolean hasPrevious; // Y a-t-il une page précédente ?
}

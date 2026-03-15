package com.example.coopachat.dtos.promotions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Réponse paginée pour la liste des promotions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionListResponseDTO {

    private List<PromotionListItemDTO> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}

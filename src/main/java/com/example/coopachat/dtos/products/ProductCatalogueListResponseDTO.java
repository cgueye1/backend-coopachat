package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour la reponse paginee du catalogue produits.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductCatalogueListResponseDTO {

    private List<ProductCatalogueItemDTO> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}

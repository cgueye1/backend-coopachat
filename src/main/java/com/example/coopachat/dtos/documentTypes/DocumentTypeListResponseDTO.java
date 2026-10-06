package com.example.coopachat.dtos.documentTypes;

import lombok.Data;
import java.util.List;

/**
 * DTO pour la réponse paginée de la liste des types de documents
 */
@Data
public class DocumentTypeListResponseDTO {
    private List<DocumentTypeDTO> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
}

package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientOrderListResponseDTO {
    private List<ClientOrderListItemDTO> orders;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}

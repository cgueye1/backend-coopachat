package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO pour les lots de commandes éligibles
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibleOrderLotDTO {
    private int lotIndex;           // index du lot (1, 2, 3...)
    private int orderCount;        // nombre de commandes
    private List<EligibleOrderDTO> orders; // liste des commandes éligibles
    private String zoneName;       // libellé de zone pour affichage (ex. "Lot 1", "Plateau", ...)
}

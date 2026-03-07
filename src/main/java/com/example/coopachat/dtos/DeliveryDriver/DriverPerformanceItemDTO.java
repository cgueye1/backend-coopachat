package com.example.coopachat.dtos.DeliveryDriver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un point du graphique Performances (ex. S1: 10 livraisons). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverPerformanceItemDTO {
    private String label;  // ex. "S1", "S2", "Mars", "Sem. 1"
    private long count;    // nombre de livraisons
}

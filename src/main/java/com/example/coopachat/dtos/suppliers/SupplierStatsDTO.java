package com.example.coopachat.dtos.suppliers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierStatsDTO {
    private long totalSuppliers;
    private long activeSuppliers;
    private long inactiveSuppliers;
}

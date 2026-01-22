package com.example.coopachat.dtos.suppliers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour afficher un fournisseur dans une liste
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierListItemDTO {

    private Long id;
    private String name;
}


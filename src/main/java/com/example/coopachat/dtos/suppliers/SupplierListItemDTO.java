package com.example.coopachat.dtos.suppliers;

import com.example.coopachat.enums.SupplierType;
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
    private String categoryNames;
    private java.util.List<String> categoriesDisplay;
    private SupplierType type;
    private String contactName;
    private String phone;
    private String email;
    private boolean isActive;
}


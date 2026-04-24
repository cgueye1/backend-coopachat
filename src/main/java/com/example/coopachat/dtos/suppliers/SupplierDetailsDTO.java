package com.example.coopachat.dtos.suppliers;

import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import java.util.List;

import com.example.coopachat.enums.SupplierType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierDetailsDTO {
    private Long id;
    private String name;
    private SupplierType type;
    private List<CategoryListItemDTO> categories;
    private String description;
    private String address;
    private String phone;
    private String email;
    private String contactName;
    private String ninea;
    private String deliveryTime;
    private Boolean isActive;
}

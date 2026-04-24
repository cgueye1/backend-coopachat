package com.example.coopachat.dtos.suppliers;

import java.util.List;

import com.example.coopachat.enums.SupplierType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSupplierDTO {
    private String name;
    private SupplierType type;
    private List<Long> categoryIds;
    private String description;
    private String address;

    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,25}$", message = "Le numéro de téléphone doit être valide")
    private String phone;

    @Email(message = "L'email doit être valide")
    private String email;
    
    private String contactName;
    private String ninea;
    private String deliveryTime;
}

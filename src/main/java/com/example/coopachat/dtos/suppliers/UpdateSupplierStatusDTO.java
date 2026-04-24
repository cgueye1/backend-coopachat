package com.example.coopachat.dtos.suppliers;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSupplierStatusDTO {
    @NotNull(message = "Le statut est obligatoire")
    private Boolean isActive;
}

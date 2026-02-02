package com.example.coopachat.dtos.delivery;

// ==================== DTO POUR CRÉER UNE ZONE ====================

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateZoneDTO {

    @NotBlank(message = "Le nom de la zone est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String zoneName;

    @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères")
    private String description;
}
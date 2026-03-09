package com.example.coopachat.dtos.reference;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer ou mettre à jour une entité "nom + description".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReferenceItemDTO {

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    private String description;
}

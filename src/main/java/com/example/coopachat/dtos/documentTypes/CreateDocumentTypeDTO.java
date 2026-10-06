package com.example.coopachat.dtos.documentTypes;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO pour la création d'un type de document prérequis
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDocumentTypeDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String name;

    private Set<String> synonyms;

    private Boolean hasExpiryDate = false;

    private Boolean isIdentityVerification = false;

    private Boolean isActive = true;
}

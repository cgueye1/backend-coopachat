package com.example.coopachat.dtos.documentTypes;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO pour les types de documents prérequis
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentTypeDTO {

    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    private String name;

    private Set<String> synonyms;

    private Boolean hasExpiryDate;

    private Boolean isIdentityVerification;

    private Boolean isActive;
}

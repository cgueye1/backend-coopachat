package com.example.coopachat.dtos.suppliers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la création d'un fournisseur
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSupplierDTO {

    @NotBlank(message = "Le nom du fournisseur est obligatoire")
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,25}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres")
    private String phone;

    @NotBlank(message = "L'adresse est obligatoire")
    private String address;

    private Boolean isActive = true;
}


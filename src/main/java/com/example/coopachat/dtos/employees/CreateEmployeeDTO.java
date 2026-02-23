
package com.example.coopachat.dtos.employees;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la création d'un salarié par un commercial
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeDTO {

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName; // Prénom

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName; // Nom

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email; // Adresse e-mail professionnelle

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,25}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres")
    private String phone; // Numéro de téléphone

    @NotBlank(message = "L'adresse est obligatoire")
    private String address; // Adresse

    @NotNull(message = "L'entreprise est obligatoire")
    private Long companyId; // ID de l'entreprise associée
}
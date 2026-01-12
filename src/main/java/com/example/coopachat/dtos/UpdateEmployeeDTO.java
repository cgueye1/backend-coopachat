package com.example.coopachat.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la modification d'un employé
 * Tous les champs sont optionnels - seuls les champs fournis seront mis à jour
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEmployeeDTO {

    @Size(min = 2, message = "Le prénom doit contenir au moins 2 caractères")
    private String firstName;

    @Size(min = 2, message = "Le nom doit contenir au moins 2 caractères")
    private String lastName;

    @Email(message = "L'email doit être valide")
    private String email;

    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,25}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres")
    private String phone;

    private String address;

    private Long companyId; // ID de l'entreprise associée
}


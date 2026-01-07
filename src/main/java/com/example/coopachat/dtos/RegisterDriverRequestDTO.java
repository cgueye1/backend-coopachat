package com.example.coopachat.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

//DTO pour la requête d'inscription d'un livreur
@Data
public class RegisterDriverRequestDTO {

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName; // Prénom

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName; // Nom

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email; // Adresse e-mail professionnelle

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^[0-9]{8,15}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres uniquement")
    private String phone; // Numéro de téléphone

}

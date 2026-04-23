package com.example.coopachat.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO pour créer un mot de passe après vérification du code d'activation
 * Utilisé lorsqu'un utilisateur crée son mot de passe pour la première fois
 */
@Data
public class SetPasswordRequestDTO {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    private String token; // Optionnel : si présent, vérifié pendant le setPassword (Flux Lien). Sinon, vérifie hasUsedActivationCode (Flux OTP).

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Le mot de passe doit contenir au moins une minuscule, une majuscule, un chiffre et un caractère spécial")
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    private String confirmPassword;
}
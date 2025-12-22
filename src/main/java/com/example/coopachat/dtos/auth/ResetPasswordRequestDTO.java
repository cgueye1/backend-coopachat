package com.example.coopachat.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO pour la réinitialisation de mot de passe
 * Utilisé lorsqu'un utilisateur réinitialise son mot de passe via le lien reçu par email
 */
@Data
public class ResetPasswordRequestDTO {

    /**
     * Token de réinitialisation reçu dans le lien par email
     */
    @NotBlank(message = "Le token de réinitialisation est obligatoire")
    private String token;

    /**
     * Nouveau mot de passe choisi par l'utilisateur
     * Doit respecter les critères de sécurité
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Le mot de passe doit contenir au moins une minuscule, une majuscule, un chiffre et un caractère spécial")
    private String newPassword;

    /**
     * Confirmation du nouveau mot de passe
     * Doit être identique au nouveau mot de passe
     */
    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    private String confirmPassword;
}


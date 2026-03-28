package com.example.coopachat.dtos.auth;

import com.example.coopachat.enums.PasswordResetChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la demande de réinitialisation de mot de passe
 * Utilisé quand l'utilisateur clique sur "Mot de passe oublié"
 */
@Data
public class ForgotPasswordRequestDTO {

    /**
     * Email professionnel de l'utilisateur
     * Un lien de réinitialisation sera envoyé à cette adresse
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    /**
     * Canal du lien dans l'email : {@link PasswordResetChannel#WEB} (navigateur) ou {@link PasswordResetChannel#MOBILE} (app).
     * Optionnel : si absent, le backend utilise {@link PasswordResetChannel#WEB}.
     * JSON : chaînes {@code "WEB"} ou {@code "MOBILE"}.
     */
    @Schema(description = "WEB (défaut si absent) ou MOBILE", allowableValues = {"WEB", "MOBILE"})
    private PasswordResetChannel channel;
}
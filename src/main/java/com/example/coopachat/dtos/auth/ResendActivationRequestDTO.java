package com.example.coopachat.dtos.auth;

import com.example.coopachat.enums.PasswordResetChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la demande de renvoi du lien d'activation
 */
@Data
public class ResendActivationRequestDTO {

    /**
     * Email de l'utilisateur
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;
}

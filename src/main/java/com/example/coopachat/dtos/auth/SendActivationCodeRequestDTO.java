package com.example.coopachat.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour demander l'envoi d'un code d'activation
 * Utilisé lorsqu'un utilisateur souhaite recevoir un code d'activation par email
 */
@Data
public class SendActivationCodeRequestDTO {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;
}


package com.example.coopachat.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour initier l'inscription mobile (livreur / salarié)
 * Contient uniquement l'email pour déclencher le flux OTP.
 */
@Data
public class RegisterMobileDTO {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;
}

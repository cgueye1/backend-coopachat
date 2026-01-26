package com.example.coopachat.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO pour vérifier un code d'activation
 * Utilisé lorsqu'un utilisateur entre le code reçu par email pour vérifier son compte
 */
@Data
public class VerifyActivationCodeRequestDTO {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le code d'activation est obligatoire")
    //^[0-9]{4}([0-9]{2})?$  exactement 4 chiffres optionnellement 2 chiffres en plus
    @Pattern(regexp = "^[0-9]{4}([0-9]{2})?$", message = "Le code doit contenir exactement 4 ou 6 chiffres")
    private String code;
}
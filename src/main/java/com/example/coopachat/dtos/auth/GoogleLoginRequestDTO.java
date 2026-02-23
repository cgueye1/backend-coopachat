package com.example.coopachat.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la connexion via Google (OAuth)
 */
@Data
public class GoogleLoginRequestDTO {
    @NotBlank(message = "Le token Google est obligatoire")
    private String idToken;
}



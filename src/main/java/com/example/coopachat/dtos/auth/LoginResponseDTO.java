package com.example.coopachat.dtos.auth;


import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO utilisé pour les réponses de connexion (login)
 * Gère aussi le cas de l’authentification à deux facteurs (2FA)
 */
@Data
@NoArgsConstructor
public class LoginResponseDTO {

    // Tokens JWT
    private String accessToken;     // Token d’accès

    // Gestion 2FA
    private String sessionId;       // ID de session temporaire
    private boolean requiresOtp;    // Indique si un code OTP est demandé

    // Cooldown pour renvoi OTP (en secondes)
    private Long resendCooldownSeconds;  // Temps restant avant de pouvoir renvoyer un code OTP


    // Informations générales
    private boolean success;        // Indique si la connexion a réussi
    private String message;         // Message de réponse
    private String email;           // Email de l'utilisateur
    private String role;          // Rôle de l'utilisateur
    private Long id;                // ID de l'utilisateur

    // Connexion avec vérification OTP
    public LoginResponseDTO(String sessionId, String email, boolean requiresOtp) {
        this.sessionId = sessionId;
        this.email = email;
        this.requiresOtp = requiresOtp;
        this.success = true;
        this.message = requiresOtp ? "Code OTP envoyé par email" : "Connexion réussie ✅";
    }

    // Connexion complète réussie (après OTP)
    public LoginResponseDTO(String accessToken, String email, String role, Long id) {
        this.accessToken = accessToken;
        this.email = email;
        this.role = role;
        this.id = id;
        this.success = true;
        this.requiresOtp = false;
        this.message = "Authentification réussie ✅";
    }

    // Réponse personnalisée
    public LoginResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.requiresOtp = false;
    }

}

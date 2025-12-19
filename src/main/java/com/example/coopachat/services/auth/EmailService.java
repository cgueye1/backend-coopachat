package com.example.coopachat.services.auth;

/**
 * Interface pour le service d'envoi d'emails
 * Gère l'envoi d'emails pour l'activation de compte et autres notifications
 */
public interface EmailService {

    /**
     * Envoie un code d'activation par email à un utilisateur
     *
     * @param email L'email du destinataire
     * @param code  Le code d'activation à 6 chiffres
     * @param firstName Le prénom de l'utilisateur (pour personnaliser l'email)
     */
    void sendActivationCode(String email, String code, String firstName);

    /**
     * Envoie un code OTP par email à un administrateur pour l'authentification 2FA
     *
     * @param email L'email de l'administrateur
     * @param code  Le code OTP à 6 chiffres
     * @param firstName Le prénom de l'administrateur (pour personnaliser l'email)
     */
    void sendOtpCode(String email, String code, String firstName);
}


package com.example.coopachat.services.auth;

import com.example.coopachat.enums.PasswordResetChannel;

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
     * Envoie un lien d'activation direct par email à un utilisateur
     *
     * @param email L'email du destinataire
     * @param code  Le code d'activation (servira de token)
     * @param firstName Le prénom de l'utilisateur
     */
    void sendActivationLink(String email, String code, String firstName);

    /**
     * Envoie un code OTP par email à un administrateur pour l'authentification 2FA
     *
     * @param email L'email de l'administrateur
     * @param code  Le code OTP à 6 chiffres
     * @param firstName Le prénom de l'administrateur (pour personnaliser l'email)
     */
    void sendOtpCode(String email, String code, String firstName);

    /**
     * Envoie un lien de réinitialisation de mot de passe par email
     *
     * @param email L'email de l'utilisateur
     * @param token Le token unique de réinitialisation
     * @param firstName Le prénom de l'utilisateur (pour personnaliser l'email)
     * @param channel WEB (URL navigateur) ou MOBILE (deep link application)
     */
    void sendPasswordResetLink(String email, String token, String firstName, PasswordResetChannel channel);

  
    /**
     * Envoie un lien d'activation direct par email à un livreur
     *
     * @param email L'email du livreur
     * @param code  Le code d'activation (token)
     * @param firstName Le prénom du livreur
     */
    void sendDriverActivationLink(String email, String code, String firstName);

    /**
     * Envoie un lien d'activation direct par email à un salarié créé par un commercial
     */
    void sendEmployeeActivationLink(String email, String code, String firstName, String commercialName, String companyName);

    /**
     * Envoie un email libre (sujet + corps texte).
     *
     * @param to      Email du destinataire
     * @param subject Sujet de l'email
     * @param body    Corps du message (texte brut, peut contenir des retours à la ligne)
     */
    void sendEmail(String to, String subject, String body);
}


package com.example.coopachat.services.auth;

import java.time.LocalDate;

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

    /**
     * Envoie un lien de réinitialisation de mot de passe par email
     *
     * @param email L'email de l'utilisateur
     * @param token Le token unique de réinitialisation
     * @param firstName Le prénom de l'utilisateur (pour personnaliser l'email)
     */
    void sendPasswordResetLink(String email, String token, String firstName);

    /**
     * Envoie un code d'activation par email à un salarié créé par un commercial
     *
     * @param email L'email du salarié
     * @param code Le code d'activation
     * @param firstName Le prénom du salarié
     * @param commercialName Le nom du commercial qui a créé l'invitation
     * @param companyName Le nom de l'entreprise
     */
    void sendEmployeeInvitation(String email, String code, String firstName, String commercialName, String companyName);
    /**
     * Envoie un code d'activation à 4 chiffres par email à un livreur
     *
     * @param email L'email du livreur
     * @param code  Le code d'activation à 4 chiffres
     * @param firstName Le prénom du livreur
     */
    void sendDriverActivationCode(String email, String code, String firstName);

    /**
     * Envoie une notification de proposition de tournée à un chauffeur
     * @param email Email du chauffeur
     * @param tourNumber Numéro de la tournée
     * @param deliveryDate Date de livraison
     * @param timeSlot Créneau horaire
     * @param driverName Nom du chauffeur
     */
    void sendTourProposalToDriver(String email, String tourNumber, LocalDate deliveryDate, String timeSlot, String driverName);


    /**
     * Envoie un email d'annulation de tournée au livreur
     */
    void sendTourCancellationToDriver(String driverEmail, String tourNumber, String reason, String driverName);

    /**
     * Notifie le Responsable Logistique par email lorsqu'un livreur soumet un signalement.
     *
     * @param rlEmail         Email du RL
     * @param driverName     Nom du livreur (prénom + nom)
     * @param reportTypeLabel Nature du signalement
     * @param comment        Commentaire du livreur (peut être vide)
     * @param orderNumber    Numéro de commande
     */
    void sendDriverReportToLogisticsManager(String rlEmail, String driverName, String reportTypeLabel, String comment, String orderNumber);
}


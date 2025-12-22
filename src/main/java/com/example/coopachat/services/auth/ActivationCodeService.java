package com.example.coopachat.services.auth;

import com.example.coopachat.enums.CodeType;

/**
 * Interface pour le service de gestion des codes d'activation
 * Gère la génération, le stockage, la validation et le nettoyage des codes d'activation
 */
public interface ActivationCodeService {

    /**
     * Génère un code d'activation à 6 chiffres
     *
     * @return Le code d'activation généré
     */
    String generateActivationCode ();


    /**
     * Génère et stocke un code d'activation pour un email
     * Supprime les anciens codes de cet email avant de créer un nouveau
     *
     * @param email L'email de l'utilisateur
     * @return Le code d'activation généré
     */
    String generateAndStoreCode(String email);


    /**
     * Valide un code d'activation pour un email
     *
     * @param email L'email de l'utilisateur
     * @param code  Le code à valider
     * @return true si le code est valide, false sinon
     */
    boolean verifyActivationCode(String email, String code);

    /**
     * Vérifie si un code d'activation a été utilisé pour un email
     *
     * @param email L'email de l'utilisateur
     * @return true si un code a été utilisé, false sinon
     */
    boolean hasUsedActivationCode(String email);


    /**
     * Marque un code d'activation comme utilisé
     *
     * @param email L'email de l'utilisateur
     * @param code  Le code à marquer comme utilisé
     */
    void markCodeAsUsed(String email, String code);

    /**
     * Supprime tous les codes d'activation expirés et non utilisés de la base de données
     */
    void cleanupExpiredCodes();

    /**
     * Supprime les codes utilisés anciens (créés il y a plus de 24 heures)
     */
    void cleanupOldUsedCodes();

    /**
     * Calcule le temps restant (en secondes) avant de pouvoir renvoyer un code
     *
     * @param email L'email de l'utilisateur
     * @param type Le type de code (ACTIVATION )
     * @return Le nombre de secondes à attendre (0 si on peut renvoyer immédiatement)
     */
    long getRemainingCooldownSecond (String email, CodeType type);



}

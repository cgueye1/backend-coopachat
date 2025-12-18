package com.example.coopachat.services.auth;

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
     * Marque un code d'activation comme utilisé
     *
     * @param email L'email de l'utilisateur
     * @param code  Le code à marquer comme utilisé
     */
    void markCodeAsUsed(String email, String code);

    /**
     * Supprime tous les codes d'activation expirés de la base de données
     */
    void cleanupExpiredCodes();

}

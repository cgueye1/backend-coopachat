package com.example.coopachat.exceptions;

/**
 * Règle métier empêchant l'action (ex. activation compte sans mot de passe défini).
 * Mappée en HTTP 400 avec le message utilisateur.
 */
public class BadRequestBusinessException extends RuntimeException {

    public BadRequestBusinessException(String message) {
        super(message);
    }
}

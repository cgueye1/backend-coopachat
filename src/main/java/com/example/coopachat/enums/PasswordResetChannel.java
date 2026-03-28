package com.example.coopachat.enums;

/**
 * Canal pour le lien de réinitialisation envoyé par email (navigateur vs app mobile).
 * JSON : utiliser les noms d'enum {@code WEB} ou {@code MOBILE}.
 */
public enum PasswordResetChannel {

    WEB,
    MOBILE
}

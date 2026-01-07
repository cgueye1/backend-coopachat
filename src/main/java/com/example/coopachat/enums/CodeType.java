package com.example.coopachat.enums;

/**
 * Type de code utilisé dans le système
 * - ACTIVATION : Code d'activation de compte (6 chiffres)
 * - PASSWORD_RESET : Token de réinitialisation de mot de passe (UUID: identifiant unique universel)
 * - EMPLOYEE_INVITATION : Token d'invitation pour salarié créé par commercial */

public enum CodeType {
    ACTIVATION,
    PASSWORD_RESET,
    EMPLOYEE_INVITATION
}
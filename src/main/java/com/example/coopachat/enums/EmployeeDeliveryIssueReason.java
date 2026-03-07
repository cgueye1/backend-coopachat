package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Raisons possibles d'un signalement de problème par le salarié (client).
 */
public enum EmployeeDeliveryIssueReason {
    LIVREUR_PAS_PASSE("Livreur n'est pas passé"),
    RETARD("Retard"),
    ABSENT("Je n'étais pas disponible"),
    ADRESSE_INCORRECTE("Adresse incorrecte"),
    PROBLEME_COMMUNICATION("Problème de communication"),
    AUTRE("Autre");

    private final String label;

    EmployeeDeliveryIssueReason(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}

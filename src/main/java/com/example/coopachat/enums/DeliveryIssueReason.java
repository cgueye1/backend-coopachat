package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Raisons possibles d'un signalement de problème de livraison par le livreur.
 */
public enum DeliveryIssueReason {
    CLIENT_ABSENT("Client absent"),
    ADRESSE_INTROUVABLE("Adresse introuvable"),
    CLIENT_REFUSE("Client refuse la livraison"),
    ACCES_IMPOSSIBLE("Accès impossible (immeuble)"),
    COLIS_ENDOMMAGE("Colis endommagé"),
    INCIDENT_ROUTE("Incident sur la route"),
    RETARD("Retard"),
    AUTRE("Autre");

    private final String label;

    DeliveryIssueReason(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}

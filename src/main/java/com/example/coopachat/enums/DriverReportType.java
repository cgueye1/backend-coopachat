package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Nature du signalement pour le formulaire "Signaler un problème" du livreur.
 */
public enum DriverReportType {
    CLIENT_ABSENT("Client absent"),
    ADRESSE_INCORRECTE("Adresse incorrecte"),
    ACCES_IMPOSSIBLE("Accès impossible"),
    COLIS_ENDOMMAGE("Colis endommagé"),
    RETARD("Retard"),
    INCIDENT_ROUTE("Incident sur la route"),
    AUTRE("Autre");

    private final String label;

    DriverReportType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}

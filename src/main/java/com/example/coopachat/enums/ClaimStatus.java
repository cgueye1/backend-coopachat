package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

//Statut d'une Réclamation
public enum ClaimStatus {
    EN_ATTENTE("En Attente"),
    VALIDE("Validé"),
    REJETE("Rejeté");

    private final String label;

    ClaimStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
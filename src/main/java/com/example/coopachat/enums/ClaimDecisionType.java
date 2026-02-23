package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type de décision lors de la validation d'une réclamation (retour produit).
 */
public enum ClaimDecisionType {

    REINTEGRATION("Réintégration au stock"),
    REMBOURSEMENT("Remboursement");

    private final String label;

    ClaimDecisionType(String label) {
        this.label = label;
    }

    @JsonValue 
    public String getLabel() {
        return label;
    }
}

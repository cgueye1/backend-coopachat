package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    /** Accepte en JSON le nom de l'enum (REINTEGRATION, REMBOURSEMENT) ou le libellé. */
    @JsonCreator
    public static ClaimDecisionType fromString(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        for (ClaimDecisionType t : values()) {
            if (t.name().equalsIgnoreCase(v) || t.label.equalsIgnoreCase(v))
                return t;
        }
        throw new IllegalArgumentException("ClaimDecisionType invalide: " + value + ". Valeurs acceptées: REINTEGRATION, REMBOURSEMENT, Réintégration au stock, Remboursement");
    }
}

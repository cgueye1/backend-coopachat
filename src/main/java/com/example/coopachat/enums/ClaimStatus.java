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

    /**
     * Accepte soit le name (EN_ATTENTE/VALIDE/REJETE) soit le label (En Attente/Validé/Rejeté).
     * Retourne null si input null/blank.
     */
    public static ClaimStatus fromString(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        // 1) Match par name (case-insensitive)
        for (ClaimStatus s : values()) {
            if (s.name().equalsIgnoreCase(v)) {
                return s;
            }
        }
        // 2) Match par label (case-insensitive)
        for (ClaimStatus s : values()) {
            if (s.label.equalsIgnoreCase(v)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Statut réclamation invalide: " + value);
    }
}
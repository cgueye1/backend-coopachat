package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CompanyStatus {
    PENDING("En attente"),
    RELAUNCHED("Relancée"),
    INTERESTED("Intéressée"),
    PARTNER_SIGNED("Partenaire signé");

    private final String label;

    CompanyStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    /** Convertit un libellé (ex. "En attente") ou le nom enum en CompanyStatus. */
    public static CompanyStatus fromLabelOrName(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        for (CompanyStatus s : values()) {
            if (s.getLabel().equalsIgnoreCase(v) || s.name().equalsIgnoreCase(v)) return s;
        }
        return null;
    }
}
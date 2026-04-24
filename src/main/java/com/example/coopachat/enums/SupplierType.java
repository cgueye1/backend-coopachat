package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SupplierType {
    GROSSISTE("Grossiste"),
    PRODUCTEUR("Producteur"),
    IMPORTATEUR("Importateur");

    private final String label;

    SupplierType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static SupplierType fromString(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        for (SupplierType t : values()) {
            if (t.name().equalsIgnoreCase(v) || t.label.equalsIgnoreCase(v)) return t;
        }
        throw new IllegalArgumentException("Type de fournisseur inconnu: " + value);
    }
}

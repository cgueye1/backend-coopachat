package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    EMPLOYEE("Salarié"),
    COMMERCIAL("Commercial"),
    LOGISTICS_MANAGER("Responsable Logistique"),
    DELIVERY_DRIVER("Livreur"),
    ADMINISTRATOR("Administrateur");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    // ========== Modification utilisateur (rôle) ==========
    // Accepte en entrée JSON le libellé (ex. "Commercial") ou le nom de l'enum (ex. "COMMERCIAL")
    // pour éviter l'erreur de désérialisation lorsque le front envoie l'un ou l'autre.
    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        for (UserRole r : values()) {
            if (r.name().equalsIgnoreCase(v) || r.label.equalsIgnoreCase(v)) return r;
        }
        throw new IllegalArgumentException("Rôle inconnu: " + value);
    }
}
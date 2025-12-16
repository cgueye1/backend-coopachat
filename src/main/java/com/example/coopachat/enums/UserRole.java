package com.example.coopachat.enums;

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
}
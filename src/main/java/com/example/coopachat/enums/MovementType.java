package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MovementType {
    ENTRY("Entrée"),
    EXIT("Sortie");

    private final String label;

    MovementType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}





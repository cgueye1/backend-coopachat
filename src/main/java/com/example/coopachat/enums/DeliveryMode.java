package com.example.coopachat.enums;

/**
 * Mode de réception de la livraison.
 */
public enum DeliveryMode {
    OFFICE("Bureau"),
    HOME("Domicile");

    private final String displayName;

    DeliveryMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
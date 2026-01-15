package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SupplierOrderStatus {
    EN_ATTENTE("En attente"),
    EN_COURS("En cours de livraison"),
    LIVREE("Livrée"),
    ANNULEE("Annulée");

    private final String label;

    SupplierOrderStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}


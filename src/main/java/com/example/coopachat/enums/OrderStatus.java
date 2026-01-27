package com.example.coopachat.enums;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    EN_ATTENTE("En attente"),
    VALIDEE("Validée"),
    EN_PREPARATION("En préparation"),
    EN_COURS_DE_LIVRAISON("En cours de livraison"),
    LIVREE("Livrée"),
    ANNULEE("Annulée");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Origine du signalement d'un problème de livraison.
 */
public enum DeliveryIssueReportSource {
    DRIVER("Livreur"),
    EMPLOYEE("Salarié");

    private final String label;

    DeliveryIssueReportSource(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}

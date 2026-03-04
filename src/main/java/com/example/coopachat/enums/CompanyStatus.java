package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CompanyStatus {
    PENDING("En attente"),
    RELAUNCHED("Relancée"),
    INTERESTED("Intéressée"),
    MEETING_SCHEDULED("Rendez-vous"),
    REFUSED("Refusée"),
    PARTNER_SIGNED("Partenaire signé");

    private final String label;

    CompanyStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
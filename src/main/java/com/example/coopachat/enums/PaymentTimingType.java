package com.example.coopachat.enums;

public enum PaymentTimingType {
    ONLINE("Paiement en ligne"),
    ON_DELIVERY("Paiement à la livraison");

    private final String label;

    PaymentTimingType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
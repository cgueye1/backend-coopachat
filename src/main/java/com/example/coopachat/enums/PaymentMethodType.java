package com.example.coopachat.enums;

public enum PaymentMethodType {
    MOBILE_MONEY("Mobile Money"),
    CREDIT_CARD("Carte bancaire"),
    CASH("Espèces");

    private final String label;

    PaymentMethodType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

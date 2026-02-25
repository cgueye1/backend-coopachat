package com.example.coopachat.enums;

public enum PaymentStatus {
    UNPAID("Impayé"),
    PENDING("En attente de confirmation"),
    PAID("Payé"),
    FAILED("Échoué"),
    REFUNDED("Remboursé");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
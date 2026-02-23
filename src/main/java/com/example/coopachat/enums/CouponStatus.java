package com.example.coopachat.enums;

public enum CouponStatus {
    PLANNED("Planifie"),
    ACTIVE("Actif"),
    EXPIRED("Expire"),
    DISABLED("Desactive");

    private final String label;

    CouponStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

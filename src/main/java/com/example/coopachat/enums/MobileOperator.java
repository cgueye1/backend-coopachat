package com.example.coopachat.enums;

public enum MobileOperator {
    ORANGE_MONEY("Orange Money"),
    WAVE("Wave"),
    FREE_MONEY("Free Money");

    private final String label;

    MobileOperator(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
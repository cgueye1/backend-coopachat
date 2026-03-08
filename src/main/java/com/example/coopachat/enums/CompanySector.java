package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CompanySector {
    TECHNOLOGY("Technologie"),
    FINANCE("Finance"),
    HEALTHCARE("Santé"),
    EDUCATION("Éducation"),
    RETAIL("Commerce de détail"),
    MANUFACTURING("Industrie manufacturière"),
    CONSTRUCTION("BTP / Construction"),
    TRANSPORTATION("Transport"),
    HOSPITALITY("Hôtellerie / Restauration"),
    ENERGY("Énergie"),
    TELECOMMUNICATIONS("Télécommunications"),
    AGRICULTURE("Agriculture"),
    FOOD_AND_BEVERAGE("Agroalimentaire"),
    PHARMACEUTICAL("Pharmaceutique"),
    AUTOMOTIVE("Automobile"),
    TEXTILE("Textile"),
    CONSULTING("Conseil"),
    REAL_ESTATE("Immobilier"),
    MEDIA("Médias"),
    GOVERNMENT("Secteur public"),
    NON_PROFIT("Association / ONG"),
    OTHER("Autre");

    private final String label;

    CompanySector(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    /** Convertit un libellé (ex. "Technologie") ou le nom enum en CompanySector. */
    public static CompanySector fromLabelOrName(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        for (CompanySector s : values()) {
            if (s.getLabel().equalsIgnoreCase(v) || s.name().equalsIgnoreCase(v)) return s;
        }
        return null;
    }
}
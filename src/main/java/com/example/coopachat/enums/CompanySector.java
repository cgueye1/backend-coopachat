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
}
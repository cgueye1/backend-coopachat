package com.example.coopachat.enums;

/**
 * Enum représentant l'état du stock d'un produit
 */
public enum EtatStock {
    SUFFISANT("Suffisant"),
    SOUS_SEUIL("Sous seuil"),
    RUPTURE("Rupture");

    private final String label;

    EtatStock(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}


package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Nature du problème pour une réclamation (formulaire "Soumettre une réclamation").
 */
public enum ClaimProblemType {
    PRODUIT_DEFECTUEUX("Produit défectueux"),
    QUANTITE_INCORRECTE("Quantité incorrecte"),
    LIVRAISON_ENDOMMAGEE("Livraison endommagée"),
    RETARD("Retard de livraison"),
    AUTRE("Autre");

    private final String label;

    ClaimProblemType(String label)
    {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}

package com.example.coopachat.enums;

import lombok.Getter;

@Getter
public enum DeliveryTourStatus {

    /** Création par RL → tournée créée, livreur assigné (RL peut encore annuler ou modifier). */
    ASSIGNEE("Assignée"),
    /** Livreur a swipé "Confirmer récupération" (impossible d'annuler). */
    EN_COURS("En cours"),
    /** Toutes commandes = LIVREE ou INCIDENT (passage automatique par le système). */
    TERMINEE("Terminée"),
    /** RL annule avant départ livreur (ASSIGNEE → ANNULEE uniquement). */
    ANNULEE("Annulée");

    private final String displayName;

    DeliveryTourStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

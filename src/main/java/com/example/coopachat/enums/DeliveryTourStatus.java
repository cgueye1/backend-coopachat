package com.example.coopachat.enums;

import lombok.Getter;

@Getter
public enum DeliveryTourStatus {

    PLANIFIEE("Planifiée"),           // Le RL a créé la tournée (brouillon)
    ASSIGNEE("Assignée"),             // Tournée assignée au livreur (notification envoyée)
    EN_COURS("En cours"),             // Le livreur a démarré la tournée
    TERMINEE("Terminée"),             // Le livreur a terminé toutes les livraisons
    ANNULEE("Annulée");               // Le Responsable Logistique a annulé la tournée

    private final String displayName;

    DeliveryTourStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

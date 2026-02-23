package com.example.coopachat.enums;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    EN_ATTENTE("En attente"),
    VALIDEE("Validée"),
    EN_PREPARATION("En préparation"),
    PRETE("Prêt à récupérer"),
    EN_COURS_DE_LIVRAISON("En cours de livraison"),
    EN_COURS("En cours"),           // Livreur en route vers le client
    ARRIVE("Arrivé"),               // Livreur sur place
    LIVREE("Livrée"),
    ECHEC_LIVRAISON("Échec livraison"),
    ANNULEE("Annulée");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
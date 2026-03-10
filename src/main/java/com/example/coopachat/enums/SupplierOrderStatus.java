package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SupplierOrderStatus {
    EN_ATTENTE("En attente"),
    EN_COURS("En cours de livraison"),
    LIVREE("Livrée"),
    ANNULEE("Annulée");

    private final String label;

    SupplierOrderStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    /**
     * Permet à l'API d'accepter à la fois :
     * - les noms d'enum (EN_ATTENTE, EN_COURS, LIVREE, ANNULEE)
     * - et les libellés français ("En attente", "Livrée", ...)
     */
    @JsonCreator
    public static SupplierOrderStatus fromJson(String value) {
        if (value == null) {
            return null;
        }
        for (SupplierOrderStatus status : SupplierOrderStatus.values()) {
            if (status.name().equalsIgnoreCase(value)
                    || status.label.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Statut de commande fournisseur invalide: " + value);
    }
}





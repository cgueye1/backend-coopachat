package com.example.coopachat.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SupplierOrderStatus {
    EN_ATTENTE("En attente"),
    EN_COURS("En cours de livraison"),
    LIVREE("Livrée"),
    ANNULEE("Annulée");

    private final String label;

    SupplierOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Accepte en JSON le nom de l'enum (EN_ATTENTE, LIVREE, ...) ou le libellé ("En attente", "Livrée", ...).
     */
    @JsonCreator
    public static SupplierOrderStatus fromJson(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        for (SupplierOrderStatus s : values()) {
            if (s.name().equalsIgnoreCase(v) || s.getLabel().equalsIgnoreCase(v)) return s;
        }
        throw new IllegalArgumentException("Statut de commande fournisseur invalide: " + value);
    }
}





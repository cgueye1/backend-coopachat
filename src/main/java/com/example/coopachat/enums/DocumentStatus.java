package com.example.coopachat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentStatus {
    PENDING("En attente de vérification"),
    VALIDATED("Validé"),
    REJECTED("Rejeté"),
    EXPIRED("Expiré");

    private final String label;
}

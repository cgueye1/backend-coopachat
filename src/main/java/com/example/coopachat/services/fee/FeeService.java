package com.example.coopachat.services.fee;

import java.math.BigDecimal;

/**
 * Service pour le calcul des frais (ex. frais de service) appliqués aux commandes.
 */
public interface FeeService {

    /**
     * Calcule la somme des montants des frais actifs 
     * Utilisé pour afficher les frais sur l'écran de paiement.
     */
    BigDecimal calculateTotalFees();
}

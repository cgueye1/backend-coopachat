package com.example.coopachat.services.fee;

import java.math.BigDecimal;

/**
 * Service pour le calcul des frais (ex. frais de service) appliqués aux commandes.
 */
public interface FeeService {

    /**
     * Calcule la somme des montants des frais actifs.
     * Utilisé pour afficher les frais sur l'écran de paiement.
     */
    BigDecimal calculateTotalFees();

    /**
     * Tarif par livraison (F CFA) configuré par l'admin.
     * L'admin crée un frais nommé "Tarif livreur" via POST /admin/fees.
     * @return montant ou 0 si non configuré
     */
    BigDecimal getDriverRatePerDelivery();
}

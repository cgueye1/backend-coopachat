package com.example.coopachat.dtos.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour le graphique "Top 5 Produits commandés" (catalogue admin).
 * Représente un produit avec son taux d'utilisation en pourcentage.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopProductUsageDTO {

    /** Nom du produit */
    private String productName;

    /**
     * Taux d'utilisation en % : part des quantités commandées de ce produit
     * par rapport au total des quantités commandées sur la période.
     * Valeur entre 0 et 100 (ex: 35.5 pour 35,5 %).
     */
    private double usagePercent;
}

package com.example.coopachat.dtos.driver;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO simplifié pour le détail d'une livraison (écran livreur).
 * Contient uniquement les infos visibles sur l'interface.
 */
@Data
public class DeliveryDetailDTO {

    // ========================================
    // INFOS PRINCIPALES
    // ========================================

    /** ID de la commande */
    private Long orderId;

    /** Numéro de commande */
    private String orderNumber;

    /** Label du statut pour affichage (ex. "EN COURS", "À LIVRER") */
    private String statusLabel;

    // ========================================
    // INFOS CLIENT
    // ========================================

    /** Nom complet du client (ex. "Lamine DIEME") */
    private String customerName;

    /** ID employé pour affichage (ex. "ID: 1001") */
    private String employeeId;

    /** Téléphone du client (ex. "77 123 45 67") */
    private String customerPhone;

    /** URL ou nom du fichier photo de profil du client */
    private String photo;

    // ========================================
    // INFOS ADRESSES
    // ========================================

    /** Adresse de livraison (ex. "Dakar Sacré Cœur") */
    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;

    // ========================================
    // INFOS LIVRAISON
    // ========================================

    /** Montant total (ex. 700 F CFA) */
    private BigDecimal totalAmount;
}

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

    /** Statut paiement */
    private String statusPaiement;

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
    /**
     * Adresse de départ (entrepôt/point de collecte)
     * Ex: "Rue 47, Pikine Ouest"
     */
    private String pickupAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;

    /** Adresse de livraison (ex. "Dakar Sacré Cœur") */
    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;

    // ========================================
    // INFOS LIVRAISON
    // ========================================

    /**
     * Temps estimé en minutes
     * Ex: 14 (calculé par Google Maps côté mobile)
     */
    private Integer estimatedMinutes;

    /** Montant total (ex. 700 F CFA) */
    private BigDecimal totalAmount;
}

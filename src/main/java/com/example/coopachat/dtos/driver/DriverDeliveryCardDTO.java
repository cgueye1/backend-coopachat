package com.example.coopachat.dtos.driver;

import com.example.coopachat.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour afficher une carte de livraison dans la liste du livreur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDeliveryCardDTO {

    // ========================================
    // INFOS ENTREPRISE/CLIENT
    // ========================================

    /**
     * Nom de l'entreprise du salarié
     */
    private String companyName;

    /**
     * Nom complet du salarié
     */
    private String customerName;

    // ========================================
    // INFOS LOCALISATION
    // ========================================

    /**
     * Adresse de livraison (format court)
     * Ex: "Dakar Sacré Cœur"
     */
    private String deliveryAddress;

    /**
     * Adresse complète formatée
     */
    private String formattedAddress;

    /**
     * Coordonnées GPS pour calcul de distance
     */
    private Double latitude;
    private Double longitude;

    // ========================================
    // INFOS COMMANDE
    // ========================================

    /**
     * ID de la commande
     */
    private Long orderId;

    /**
     * Numéro de commande
     * Ex: "CMD-1232"
     */
    private String orderNumber;

    /**
     * Label du statut pour affichage
     * Ex: "À livrer", "En cours", "Livrée"
     */
    private String statusLabel;

    // ========================================
    // INFOS TIMING
    // ========================================

    /**
     * Créneau horaire
     * Ex: "09:00 - 10:00"
     */
    private String timeSlot;

    /**
     * Date de livraison
     */
    private LocalDate deliveryDate;


}

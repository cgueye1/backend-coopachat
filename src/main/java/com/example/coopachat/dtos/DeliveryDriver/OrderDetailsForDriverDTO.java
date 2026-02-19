package com.example.coopachat.dtos.DeliveryDriver;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Détail d'une commande pour l'écran "Détail commande" du livreur :
 * infos commande, produits, total, client, adresse, suivi (timeline).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailsForDriverDTO {

    private String orderNumber;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate orderDate;
    private String statusLabel;
    private int productCount;
    /** Montant total des produits (sous-total). */
    private BigDecimal productsTotal;
    /** Montant livraison (frais de service). */
    private BigDecimal deliveryAmount;
    /** Total à percevoir (produits + livraison) – pour affichage "Montant reçu" (espèces). */
    private BigDecimal totalAmount;
    private List<OrderItemForDriverDTO> items;

    private String clientName;
    private String deliveryAddress;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate deliveryDate;

    /** Commande créée (pour timeline, format HH:mm). */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime createdAt;
    /** Commande validée / mise en tournée (pour timeline). */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime validatedAt;
    /** En cours de livraison (pour timeline). */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryStartedAt;
    /** Livreur arrivé sur place (pour timeline). */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryArrivedAt;
    /** Commande livrée (pour timeline). */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryCompletedAt;

    /** Méthode de paiement (ex. Espèces, Mobile Money, Carte bancaire) pour "Informations de paiement". */
    private String paymentMethodLabel;
    /** Statut du paiement (ex. Payé, Impayé) pour que le livreur vérifie avant de finaliser. */
    private String paymentStatusLabel;
}

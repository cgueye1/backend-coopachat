package com.example.coopachat.dtos.driver;

import com.example.coopachat.dtos.order.ClientOrderItemDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Détail complet d'une commande pour le livreur.
 * Règle métier : accessible uniquement si la commande est LIVREE et appartient au livreur connecté.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDeliveredOrderDetailsDTO {
    private Long orderId;
    private String orderNumber;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate orderDate;
    private String statusLabel;

    /** Infos salarié */
    private String employeeName;

    /** Produits */
    private int productCount;
    private BigDecimal totalAmount;
    private List<ClientOrderItemDTO> items;

    /** Adresse + date de livraison */
    private String deliveryAddress;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate deliveryDate;

    /** Timeline */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime validatedAt;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime pickupStartedAt;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryStartedAt;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryArrivedAt;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryCompletedAt;

    /** Paiement */
    private String paymentTimingType;
    private String paymentStatusLabel;
}


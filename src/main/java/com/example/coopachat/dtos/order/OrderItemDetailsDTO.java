package com.example.coopachat.dtos.order;

import com.example.coopachat.dtos.products.ProductPreviewDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTO pour afficher les détails d'une commande salarié (sérialisé en JSON pour le front). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDetailsDTO {

    private String orderNumber;
    private LocalDate validationDate;
    private String employeeName;
    private String status;
    /** Nom/prénom de l'acteur ayant positionné le statut courant. */
    private String currentStatusChangedByName;
    /** Date/heure à laquelle le statut courant a été posé. */
    private LocalDateTime currentStatusChangedAt;
    /** Rôle de l'acteur ayant positionné le statut courant (optionnel). */
    private String currentStatusChangedByRole;
    /** Nom complet du livreur qui a livré la commande (null si aucun livreur associé). */
    private String driverName;
    /** Téléphone du livreur (Users.phone), null si inconnu. */
    private String driverPhone;
    /** Raison saisie par le livreur en cas d'échec de livraison (Order.failureReason). */
    private String failureReason;
    private List<ProductPreviewDTO> listProducts;
}

package com.example.coopachat.dtos.delivery;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** DTO pour mise à jour d'une tournée (date, livreur, véhicule, notes, commandes à conserver). Statut ASSIGNEE uniquement. */
@Data
public class UpdateDeliveryTourDTO {
    /** Nouvelle date de livraison (optionnel = inchangé). */
    private LocalDate deliveryDate;
    /** Nouveau livreur (optionnel = inchangé). */
    private Long driverId;
    private String vehicleInfo;         // "Type / Plaque"
    private String notes;              // Commentaires
    /** IDs des commandes à garder dans la tournée. Si fourni et vide → erreur "Pas de tournée sans commande". */
    private List<Long> orderIds;
}
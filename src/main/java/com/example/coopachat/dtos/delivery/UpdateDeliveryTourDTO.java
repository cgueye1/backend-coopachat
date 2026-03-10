package com.example.coopachat.dtos.delivery;

import com.example.coopachat.enums.DeliveryTourStatus;
import lombok.Data;

import java.util.List;

/** DTO pour mise à jour d'une tournée (véhicule, notes, commandes à conserver). */
@Data
public class UpdateDeliveryTourDTO {
    private DeliveryTourStatus status;
    private String vehicleInfo;         // "Type / Plaque"
    private String notes;              // Commentaires
    /** IDs des commandes à garder dans la tournée. Si fourni et vide → erreur "Pas de tournée sans commande". */
    private List<Long> orderIds;
}
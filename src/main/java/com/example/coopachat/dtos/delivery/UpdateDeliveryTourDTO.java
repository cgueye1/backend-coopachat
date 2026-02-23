package com.example.coopachat.dtos.delivery;

import com.example.coopachat.enums.DeliveryTourStatus;
import lombok.Data;

//DTO pour Mis à jour Tournée
@Data
public class UpdateDeliveryTourDTO {
    private DeliveryTourStatus status;
    private String vehicleInfo;         // "Type/Plaque"
    private String notes;               // Commentaires
}
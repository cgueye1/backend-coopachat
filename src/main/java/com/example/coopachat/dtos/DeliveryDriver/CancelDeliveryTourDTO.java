package com.example.coopachat.dtos.DeliveryDriver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

//DTO Motif d'annulation Tournée
@Data
public class CancelDeliveryTourDTO {

    @NotBlank(message = "Le motif d'annulation est obligatoire")
    @Size(max = 500, message = "Le motif ne peut dépasser 500 caractères")
    private String reason; // Motif d'annulation

}
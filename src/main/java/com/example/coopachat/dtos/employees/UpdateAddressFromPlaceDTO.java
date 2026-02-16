package com.example.coopachat.dtos.employees;

import com.example.coopachat.enums.DeliveryMode;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
    //DTO pour mettre à jour une adresse à partir d'un lieu
@Data
public class UpdateAddressFromPlaceDTO {
    @NotBlank(message = "placeId obligatoire")
    private String placeId;//ID du lieu
    private Long addressId;//ID de l'adresse
    private DeliveryMode deliveryMode;//Mode de livraison
    private boolean primary;//Si l'adresse est principale
}
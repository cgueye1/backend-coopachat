package com.example.coopachat.dtos.DeliveryDriver;

import com.example.coopachat.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un élément de la liste "Mes livraisons" du livreur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDeliveryListItemDTO {

    private Long orderId;//ID de la commande
    private String orderNumber;//Numéro de la commande
    private String clientName;//Nom du client
    private String address;//Adresse de livraison
    private Double latitude;//Latitude
    private Double longitude;//Longitude
    private String timeSlot;//Créneau horaire
    private OrderStatus status;//Statut de la commande
    private Long tourId;//ID de la tournée
}

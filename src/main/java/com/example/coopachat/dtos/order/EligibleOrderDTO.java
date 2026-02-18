package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibleOrderDTO {
    private Long orderId; // id de la commande
    private String orderNumber; // numéro de la commande
    private String customerName; // nom du client
    private String formattedAddress; // adresse formatée
}

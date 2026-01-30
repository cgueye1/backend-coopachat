package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//DTO pour finaliser la commande
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateOrderDTO {
    private Long deliveryOptionId; // Option choisie (1, 2 ou 3)
    private String couponCode;// code promo ,Optionnel, peut être null
}

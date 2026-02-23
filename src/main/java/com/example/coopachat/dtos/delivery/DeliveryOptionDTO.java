package com.example.coopachat.dtos.delivery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOptionDTO {
    private Long id;                // pour le salarié : valeur à envoyer dans createOrder (deliveryOptionId)
    private String name;            // "Hebdomadaire"
    private String description;     // "1 fois par semaine"
    private Boolean isActive;       // true/false
}
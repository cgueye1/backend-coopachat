package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibleOrderDTO {
    private Long orderId;//id de la commande en base
    private String orderNumber;//numéro de la commande
    private String customerName;//nom du client
    private String formattedAddress;//adresse formatée

    //Informatif pour le RL 
    private List<String> preferredDays;       // ex: ["LUNDI", "MARDI"]
    private String preferredTimeSlot;         // ex: "Matin (8h-12h)"
    private String preferredDeliveryMode;     // ex: "Domicile"
    private Boolean matchesPreferences;       // true si la tournée correspond aux préférences
    private Boolean hasPreferences;           // false si le salarié n'a pas renseigné de préférences

    /** Nombre de jours de retard (date de livraison prévue dépassée). null si pas en retard. */
    private Integer daysOverdue;
}

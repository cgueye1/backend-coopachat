package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibleOrderDTO {
    private String orderNumber;
    private String customerName;
    /** Zone = ville + quartier de l'adresse de livraison (ex. "Dakar, Mermoz"). */
    private String zone;
}

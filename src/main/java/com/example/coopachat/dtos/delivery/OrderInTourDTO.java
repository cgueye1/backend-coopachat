package com.example.coopachat.dtos.delivery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Une commande dans la liste des commandes d'une tournée (détails tournée).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInTourDTO {
    private Long orderId;
    private String orderNumber;
    private String employeeName;
    private String deliveryAddress;
}

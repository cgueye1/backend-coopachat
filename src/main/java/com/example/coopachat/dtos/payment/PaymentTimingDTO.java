package com.example.coopachat.dtos.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour un moment de paiement (liste ou détail). id, name, description optionnelle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTimingDTO {
    private Long id;
    private String name;
    private String description;
}

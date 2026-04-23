package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {

   // Indique si la demande a été traitée correctement côté backend
    private boolean success;

    // Message informatif destiné au frontend (ex: instruction ou confirmation)
    private String message;

    // Référence unique générée côté backend (order_number / partner_transaction_id)
    private String transactionReference;

    // Montant total à payer (sous-total + frais de service) - utile côté UI
    private BigDecimal amountPaid;

    // URL de la WebView TouchPay  à ouvrir sur le mobile
    private String paymentUrl;

    // Token JWT à injecter dans la WebView 
    private String jwtToken;

}

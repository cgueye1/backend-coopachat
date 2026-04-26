package com.example.coopachat.dtos.Payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Réponse du endpoint bridge (pont de connexion)consommé par la page touchpay-bridge.html.
 * Cette réponse contient uniquement ce dont le script TouchPay a besoin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBridgeResponseDTO {
    private String merchantToken;
    private String transactionReference;
    private String agencyCode;
    private String serviceId;
    private String hostedScriptUrl;
    private BigDecimal amount;
    private String city;
    private String successRedirectUrl;
    private String failedRedirectUrl;
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;
    private String customerPhone;
}


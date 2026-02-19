package com.example.coopachat.dtos.order;

import com.example.coopachat.enums.MobileOperator;
import com.example.coopachat.enums.PaymentMethodType;
import com.example.coopachat.enums.PaymentTimingType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//DTO pour le paiement d'une commande
@Data
public class ProcessPaymentDTO {

    @NotNull(message = "Mode de paiement obligatoire")
    private PaymentMethodType paymentMethod;

    @NotNull(message = "Moment de paiement obligatoire")
    private PaymentTimingType paymentTiming;

    // ═══════════════════════════════════════
    // MOBILE MONEY
    // ═══════════════════════════════════════

    /** Opérateur (obligatoire si Mobile Money) */
    private MobileOperator mobileOperator;

    // ═══════════════════════════════════════
    // CARTE BANCAIRE
    // ═══════════════════════════════════════
    /** Numéro de carte (simulation uniquement) */
    private String cardNumber;

    /** Date d'expiration (simulation uniquement) */
    private String cardExpiry;

    /** CVV (simulation uniquement) */
    private String cardCvv;
}


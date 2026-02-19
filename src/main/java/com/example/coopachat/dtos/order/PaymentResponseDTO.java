package com.example.coopachat.dtos.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Réponse après traitement d'un paiement (simulation).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private boolean success;
    private String message;
    private String transactionReference;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime paidAt;
    private BigDecimal amountPaid;
}

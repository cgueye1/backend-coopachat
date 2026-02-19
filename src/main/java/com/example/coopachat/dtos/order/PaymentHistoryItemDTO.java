package com.example.coopachat.dtos.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Élément de l'historique des paiements (écran "Historique des paiements").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryItemDTO {
    private String orderNumber;

    @JsonFormat(pattern = "dd MMM, HH:mm", locale = "fr")//MMM -> Mois en lettres abrégées
    private LocalDateTime paidAt;

    private BigDecimal amountPaid;

    /** Ex. "Carte bancaire", "Espèces", "Mobile Money" */
    private String paymentMethodLabel;

    /** Renseigné si Mobile Money (ex. "Orange Money"), sinon null */
    private String mobileOperatorLabel;
}

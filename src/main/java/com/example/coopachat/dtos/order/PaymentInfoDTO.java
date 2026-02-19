package com.example.coopachat.dtos.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Infos de paiement pour l'écran "Payer la facture" (sous-total, frais de service, total, statut).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfoDTO {
    private String orderNumber;//Numéro de la commande
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;//Date de la commande 
    private int productCount;//Nombre d'articles 
    private BigDecimal subtotal;//sous-total
    private BigDecimal serviceFee;//Frais de service 
    private BigDecimal total;// total
    private String paymentStatus;//statut de paiement 
}

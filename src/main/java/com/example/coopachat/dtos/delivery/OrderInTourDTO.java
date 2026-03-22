package com.example.coopachat.dtos.delivery;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Une commande dans la liste des commandes d'une tournée (détails tournée).
 */
@Data
@NoArgsConstructor
public class OrderInTourDTO {
    private Long orderId;
    private String orderNumber;
    /** Nom complet (compatibilité). */
    private String employeeName;
    private String employeeFirstName;
    private String employeeLastName;
    /** Nom de l'entreprise du salarié. */
    private String companyName;
    private String deliveryAddress;
    /** Montant total de la commande. */
    private BigDecimal totalAmount;
    /** Code statut commande (ex. EN_COURS, LIVREE, ECHEC_LIVRAISON). */
    private String orderStatus;
    /** Libellé français du statut. */
    private String orderStatusLabel;
    /** Libellé du mode de paiement (ex. Mobile Money). */
    private String paymentMethodLabel;
}

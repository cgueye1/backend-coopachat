package com.example.coopachat.dtos.Payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 *  Représente les données envoyées par InTouch (callback)
 *   après le traitement d’un paiement.
 */
@Data
public class InTouchCallbackRequest {

    @JsonProperty("service_id")
    private String serviceId;//

    @JsonProperty("gu_transaction_id")
    private String guTransactionId;//Identifiant unique de la transaction côté InTouch

    @JsonProperty("status")
    private String status;//Statut du paiement

    @JsonProperty("partner_transaction_id")
    private String partnerTransactionId;//Référence de transaction côté partenaire (Permet de retrouver le paiement dans notre base de données)

    @JsonProperty("call_back_url")
    private String callBackUrl;//URL de callback appelée par InTouch

    private String commission;//Commission prélevée
    private String message;// Message retourné par InTouch (succès ou erreur)
}

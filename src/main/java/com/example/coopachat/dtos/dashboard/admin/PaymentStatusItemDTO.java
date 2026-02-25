package com.example.coopachat.dtos.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un élément du graphique "Paiements par statut" : libellé du statut et nombre.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusItemDTO {

    /** Libellé du statut (ex. Payé, En attente, Échoué). */
    private String statusLabel;

    /** Nombre de paiements dans ce statut sur la période. */
    private long count;
}

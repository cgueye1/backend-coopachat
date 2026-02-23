package com.example.coopachat.dtos.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Élément de la liste "Mes commandes" (profil client).
 * - driver : renseigné uniquement si statut = EN_COURS ou ARRIVE (en cours de livraison).
 * - rating : renseigné uniquement si statut = LIVREE et que le client a déjà noté (affichage des étoiles).
 * - canRate : true si LIVREE et pas encore noté (bouton "Noter").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientOrderListItemDTO {
    private Long orderId;
    private String orderNumber;
    private String deliveryAddress;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate orderDate;
    private int itemCount;
    private String statusLabel;

    /** Infos livreur : présent uniquement si statut = En cours de livraison / Arrivé. Sinon null. */
    private DriverInfoForClientDTO driver;

    /** Note (1-5) affichée si commande livrée et déjà notée. Sinon null. */
    private Integer rating;

    /** true si commande livrée et pas encore notée (afficher bouton "Noter"). */
    private boolean canRate;
}

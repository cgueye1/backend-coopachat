package com.example.coopachat.dtos.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour un élément de la liste des commandes salariés
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEmployeeListItemDTO {

    private Long id; // ID de la commande
    private String orderNumber; // Numéro de commande (CMD-XXX)
    private String employeeName; // Nom complet du salarié

    /** Date à laquelle le salarié a passé la commande (createdAt). */
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate orderDate;

    /** Date à laquelle la commande est passée à son statut actuel (ex. date de validation, de livraison, d'échec). */
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate statusDate;

    private List<String> products; // Liste des produits concernés

    private String deliveryFrequency; // Fréquence de livraison

    private String status; // Statut de la commande (label de l'enum)

    /** Raison de l'échec de livraison (pour statut ECHEC_LIVRAISON). Visible directement dans la liste pour le RL. */
    private String failureReason;
}

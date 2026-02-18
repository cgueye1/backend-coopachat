package com.example.coopachat.dtos.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Détail d'une commande pour le profil client (écran "Détails" / "Voir plus").
 * driver : renseigné uniquement si statut = EN_COURS ou ARRIVE. Pas de rating/canRate ici (réservé à la liste "Mes commandes").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientOrderDetailsDTO {
    private Long orderId;
    private String orderNumber;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate orderDate;
    private String statusLabel;
    private int productCount;
    private BigDecimal totalAmount;
    private List<ClientOrderItemDTO> items;

    private String deliveryAddress;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate deliveryDate;

    /** Timeline (suivi de commande). */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime validatedAt;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryStartedAt;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryArrivedAt;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryCompletedAt;

    /** Infos livreur : présent uniquement si En cours de livraison / Arrivé. */
    private DriverInfoForClientDTO driver;
}

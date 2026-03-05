package com.example.coopachat.entities;

import com.example.coopachat.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNumber; // Numéro de la commande

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee; // le salarié concerné

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // utilisateur connecté (celui qui a passé la commande)

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();//Liste des produits concernés

    @ManyToOne
    @JoinColumn(name = "delivery_option_id")
    private DeliveryOption deliveryOption;//les options de livraison

    private LocalDate deliveryDate;//La date de livraison

    @ManyToOne
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;// Coupon appliqué si ça existe

    @Column(nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO; //Prix total de tous les produits

    @Column(nullable = false)
    private Integer totalItems = 0; // Nombre total d'articles

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.EN_ATTENTE;//status de la commande , en-attente par défaut

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;//le paiement associé

    @ManyToOne
    @JoinColumn(name = "delivery_tour_id" )
    private DeliveryTour deliveryTour;//la tournée qu'il appartient

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt; // Date de création

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification

    /** Heure à laquelle la commande a été validée (mise en tournée par le RL). */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime validatedAt;

    /** Heure à laquelle le livreur a confirmé la récupération des colis (statut EN_PREPARATION). */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    @Column(name = "pickup_started_at")
    private LocalDateTime pickupStartedAt;

    /** Heure à laquelle le livreur a démarré la livraison (statut EN_COURS). Affichage heure:minute uniquement. */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryStartedAt;

    /** Heure à laquelle le livreur a confirmé son arrivée sur place (statut ARRIVE). */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryArrivedAt;

    /** Heure à laquelle le colis a été remis au client (statut LIVREE). Affichage heure:minute uniquement. */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime deliveryCompletedAt;

    /**
     * Raison de l'échec de livraison
     */
    @Column(name = "failure_reason")
    private String failureReason;

    /**
     * Date/heure du signalement d'échec
     */
    @Column(name = "failure_reported_at")
    private LocalDateTime failureReportedAt;
}
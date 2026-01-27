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
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // le user concerné

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
    private BigDecimal totalPrice = BigDecimal.ZERO; //Prix total produits (sous-total)

    @Column(nullable = false)
    private Integer totalItems = 0; // Nombre total d'articles

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.EN_ATTENTE;//status de la commande , en-attente par défaut

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt; // Date de création

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification
}
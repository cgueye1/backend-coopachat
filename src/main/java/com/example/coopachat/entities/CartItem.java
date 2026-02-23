package com.example.coopachat.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Employé propriétaire du panier
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // Utilisateur (pour colonne user_id en base si présente)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    //  Quel produit ?
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    //Combien ? (1, 2, 3...)
    @Column(nullable = false)
    private Integer quantity = 1;

    //  Prix UNITAIRE au moment de l'ajout
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    // Prix PROMO unitaire (si le produit avait une promo)
    @Column(name = "promo_price")
    private BigDecimal promoPrice;

    // Y a-t-il une promo ?
    @Column(name = "has_promo")
    private Boolean hasPromo = false;

    // Quand ajouté ?
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt; // Date de création

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification

    private BigDecimal subtotal;

    @PrePersist
    @PreUpdate
    public void calculateSubtotal() {
        // Prix effectif = promo si existe, sinon prix normal
        BigDecimal effectivePrice = (hasPromo && promoPrice != null)
                ? promoPrice
                : unitPrice;

        // Calculer sous-total
        this.subtotal = effectivePrice.multiply(BigDecimal.valueOf(quantity));
    }
}
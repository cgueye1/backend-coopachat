package com.example.coopachat.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order; // Appartient à une commande

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product; // Quel produit

    private Integer quantity; // Combien

    private BigDecimal unitPrice; // Prix normal au moment de la commande
    private BigDecimal promoPrice; // Prix promo (si applicable)
    private BigDecimal subtotal; // Quantité × prix

    @PrePersist
    public void calculateSubtotal() {
        BigDecimal priceToUse = promoPrice != null ? promoPrice : unitPrice;
        if (priceToUse != null && quantity != null) {
            subtotal = priceToUse.multiply(BigDecimal.valueOf(quantity));
        }
    }

}
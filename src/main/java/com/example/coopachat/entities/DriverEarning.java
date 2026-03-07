package com.example.coopachat.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Gains du livreur par livraison (500 F par livraison livrée).
 * Créé automatiquement quand une commande passe en LIVREE.
 */
@Entity
@Table(name = "driver_earnings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Montant crédité (ex. 500 F CFA). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime earnedAt;
}

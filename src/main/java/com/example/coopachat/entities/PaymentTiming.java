package com.example.coopachat.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Moment du paiement (ex. Paiement en ligne, Paiement à la livraison).
 * Géré par l'admin (pas d'enum) : id, name, description optionnelle.
 */
@Entity
@Table(name = "payment_timings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTiming {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;
}

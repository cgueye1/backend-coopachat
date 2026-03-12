package com.example.coopachat.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Avis laissé par un client (employé) sur le livreur après une livraison.
 * Une seule note par commande ; le client ne peut plus modifier après envoi.
 */
@Entity
@Table(name = "driver_reviews", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverAvis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(nullable = false)
    private Integer rating; // 1 à 5 étoiles

    /** Tags (ex. Ponctuel, Professionnel, Sympathique) stockés en liste. */
    @ElementCollection
    @CollectionTable(name = "driver_review_tags", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(length = 500)
    private String comment;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

package com.example.coopachat.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Référentiel centralisé des zones de livraison
 * Géré par l'ADMIN
 */
@Entity
@Table(name = "delivery_zone_references")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryZoneReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String zoneName; // Ex: "Dakar", "Pikine", "Guédiawaye"

    @Column(length = 255)
    private String description; // Ex: "Zone centrale de Dakar"

    @Column(nullable = false)
    private Boolean active = true; // Permet de désactiver une zone sans la supprimer

    // ==================== MÉTADONNÉES ====================

    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private Users createdBy; // Admin qui a créé la zone

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}

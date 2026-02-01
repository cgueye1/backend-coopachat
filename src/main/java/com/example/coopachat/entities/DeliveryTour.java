package com.example.coopachat.entities;

import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.TimeSlot;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//Tournée de livraison
@Entity
@Table(name = "delivery_tours")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String tourNumber; // Numéro unique de la tournée (ex: TOUR-2025-001)

    // ==================== INFORMATIONS DE BASE ====================

    @Column(nullable = false)
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate deliveryDate; // Date de livraison

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSlot timeSlot; // Créneau horaire (MORNING, AFTERNOON, ALL_DAY)

    @Column(nullable = false)
    private String deliveryZone; // Zone de livraison (ex: "Dakar")

    // ==================== ACTEURS ====================

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver; // Livreur/Chauffeur assigné

    @Column(nullable = false)
    private String vehiclePlate; // Plaque d'immatriculation du véhicule (ex: AA-1234-AB)

    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private Users createdBy; // Responsable Logistique qui a créé la tournée

    // ==================== COMMANDES ====================

    @OneToMany(mappedBy = "deliveryTour")
    private List<Order> orders = new ArrayList<>(); // Liste des commandes incluses dans cette tournée

    // ==================== STATUT ====================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryTourStatus status = DeliveryTourStatus.PLANIFIEE; // Statut par défaut

    // ==================== NOTES ====================

    @Column(columnDefinition = "TEXT")
    private String notes; // Notes optionnelles du responsable logistique

    // ==================== MÉTADONNÉES ====================

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt; // Date de création

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime confirmedAt; // Date de confirmation par le livreur

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime startedAt; // Date de démarrage de la tournée

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime completedAt; // Date de fin de la tournée

    // ==================== MÉTHODES UTILES ====================

    /**
     * Calcule le nombre total de commandes dans la tournée
     */
    public int getTotalOrders() {
        return orders != null ? orders.size() : 0;
    }

    /**
     * Vérifie si la tournée peut être modifiée
     */
    public boolean canBeModified() {
        return status == DeliveryTourStatus.PLANIFIEE ||
                status == DeliveryTourStatus.PROPOSEE;
    }

    /**
     * Vérifie si la tournée peut être confirmée
     */
    public boolean canBeConfirmed() {
        return status == DeliveryTourStatus.PROPOSEE;
    }

    /**
     * Vérifie si la tournée peut être démarrée
     */
    public boolean canBeStarted() {
        return status == DeliveryTourStatus.CONFIRMEE;
    }
}

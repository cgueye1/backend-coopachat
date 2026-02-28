package com.example.coopachat.entities;

import com.example.coopachat.entities.Driver;
import com.example.coopachat.entities.Order;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.DeliveryTourStatus;
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

@Entity
@Table(name = "delivery_tours")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numéro unique de la tournée (ex. DT-2025-001). */
    @Column(unique = true, nullable = false)
    private String tourNumber;

    // ==================== INFORMATIONS DE BASE ====================

    /** Date prévue de livraison pour cette tournée. */
    @Column(nullable = false)
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate deliveryDate;

    // ==================== ACTEURS ====================

    /** Livreur assigné à la tournée. */
    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    /** Type de véhicule et plaque (ex. "Camion - AB-123-CD"). */
    @Column(nullable = false)
    private String vehicleTypePlate;

    /** Utilisateur (RL) ayant créé la tournée. */
    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private Users createdBy;

    // ==================== COMMANDES ====================

    /** Commandes incluses dans cette tournée. */
    @OneToMany(mappedBy = "deliveryTour")
    private List<Order> orders = new ArrayList<>();

    // ==================== STATUT ====================

    /** État actuel de la tournée (ASSIGNEE, EN_COURS, LIVREE, ANNULEE). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryTourStatus status = DeliveryTourStatus.ASSIGNEE;

    // ==================== NOTES ====================

    /** Notes libres sur la tournée. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Motif d'annulation si la tournée est annulée. */
    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    // ==================== MÉTADONNÉES ====================

    /** Date de création de l'enregistrement. */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Date de dernière modification. */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Dernier utilisateur ayant modifié la tournée. */
    @ManyToOne
    @JoinColumn(name = "updated_by_id", nullable = false)
    private Users updatedBy;

    /** Date/heure d'annulation si la tournée est annulée. */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime cancelledAt;

    /** Utilisateur ayant annulé la tournée. */
    @ManyToOne
    @JoinColumn(name = "cancelled_by_id")
    private Users cancelledBy;

    /**
     * Date/heure de début (quand livreur confirme récupération)
     */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime startedAt;

    /** Utilisateur (livreur) ayant confirmé la tournée. */
    @ManyToOne
    @JoinColumn(name = "confirmed_by_id")
    private Users startedBy;


    /** Date/heure de fin de la tournée (toutes livraisons terminées ou tournée clôturée). */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime completedAt;

    // ==================== MÉTHODES UTILES ====================

    /** RL peut modifier uniquement si tournée assignée (avant que le livreur parte). */
    public boolean canBeModified() {
        return status == DeliveryTourStatus.ASSIGNEE;
    }

    public boolean canBeStarted() {
        return status == DeliveryTourStatus.ASSIGNEE;
    }
}
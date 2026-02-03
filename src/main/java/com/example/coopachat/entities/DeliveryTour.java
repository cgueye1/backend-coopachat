package com.example.coopachat.entities;

import com.example.coopachat.entities.Driver;
import com.example.coopachat.entities.Order;
import com.example.coopachat.entities.Users;
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
    private String tourNumber;

    // ==================== INFORMATIONS DE BASE ====================

    @Column(nullable = false)
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSlot timeSlot;

    @ManyToOne
    @JoinColumn(name = "delivery_zone_id", nullable = false)
    private DeliveryZoneReference deliveryZone;

    // ==================== ACTEURS ====================

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;


    @Column(nullable = false)
    private String vehicleTypePlate;

    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private Users createdBy;

    // ==================== COMMANDES ====================

    @OneToMany(mappedBy = "deliveryTour")
    private List<Order> orders = new ArrayList<>();

    // ==================== STATUT ====================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryTourStatus status = DeliveryTourStatus.PLANIFIEE;

    // ==================== NOTES ====================

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ==================== MÉTADONNÉES ====================

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime confirmedAt;

    @ManyToOne
    @JoinColumn(name = "confirmed_by_id", nullable = false)
    private Users confirmedBy;


    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime completedAt;

    // ==================== MÉTHODES UTILES ====================

    public boolean canBeModified() {
        return status == DeliveryTourStatus.PLANIFIEE ||
                status == DeliveryTourStatus.PROPOSEE;
    }

    public boolean canBeConfirmed() {
        return status == DeliveryTourStatus.PROPOSEE;
    }

    public boolean canBeStarted() {
        return status == DeliveryTourStatus.CONFIRMEE;
    }
}
package com.example.coopachat.entities;

import com.example.coopachat.enums.TimeSlot;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "driver_availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== LIVREUR ====================
    @OneToOne
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private Driver driver; // Un livreur = une disponibilité

    // ==================== JOURS DE DISPONIBILITÉ ====================
    // STOCKE LA LISTE DES JOURS DANS UNE TABLE SÉPARÉE LIÉE À CETTE ENTITÉ
    @ElementCollection
    @CollectionTable(
            name = "driver_available_days",
            joinColumns = @JoinColumn(name = "availability_id")  // Clé étrangère vers ( DriverAvailability)
    )
    @Column(name = "day_of_week")
    private Set<String> availableDays = new HashSet<>();

    // ==================== CRÉNEAU HORAIRE (UN SEUL CHOIX) ====================
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_time_slot")
    private TimeSlot preferredTimeSlot;

    // ==================== MÉTADONNÉES ====================
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt; // Date de création

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification

    // ==================== MÉTHODES UTILES ====================

    /**
     * Vérifie si le livreur est disponible un jour donné
     */
    public boolean isAvailableOn(String day) {
        return availableDays.contains(day);
    }

}
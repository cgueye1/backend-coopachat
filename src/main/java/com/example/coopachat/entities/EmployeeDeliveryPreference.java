package com.example.coopachat.entities;

import com.example.coopachat.enums.DeliveryMode;
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
@Table(name = "user_delivery_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class  EmployeeDeliveryPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== UTILISATEUR ====================
    @OneToOne
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee; // Un salarié = une préférence

    // ==================== JOURS DE DISPONIBILITÉ ====================

    @ElementCollection
    // ↑ Indique à JPA que ce champ est une COLLECTION d'éléments,  - "Element" = élément simple (String, Enum, etc.) plutôt qu'une entité complète
    // - "Collection" = on peut en stocker plusieurs (Set, List)
    @CollectionTable(
            name = "preferred_delivery_days",  // ← Nom de la TABLE qui stockera ces jours
            joinColumns = @JoinColumn(         // ← Comment relier à la table principale
                    name = "preference_id"         // ← Nom de la colonne de liaison
            )
    )
    @Column(name = "day_of_week")  // ← Nom de la colonne dans la table preferred_delivery_days
    private Set<String> preferredDays = new HashSet<>(); //  Set<String> = Collection de jours sans doublons

    // ==================== CRÉNEAUX HORAIRES ====================
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_time_slot")
    private TimeSlot preferredTimeSlot;

    // ==================== MODE DE RÉCEPTION ====================
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode")
    private DeliveryMode deliveryMode;

    // ==================== MÉTADONNÉES ====================
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ==================== MÉTHODES UTILES ====================

    /**
     * Vérifie si l'utilisateur est disponible un jour donné
     */
    public boolean isAvailableOn(String day) {
        return preferredDays.contains(day);
    }

}
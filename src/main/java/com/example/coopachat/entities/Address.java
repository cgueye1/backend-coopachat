package com.example.coopachat.entities;

import com.example.coopachat.enums.DeliveryMode;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Adresse de livraison du salarié.
 * Peut contenir des coordonnées GPS (géocodage type Yango/Google).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    private DeliveryMode deliveryMode; // "Domicile", "Bureau", "Autre"

    private boolean isPrimary;// Adresse principale

    /** Adresse formatée (Google Places). Si présent, on utilise uniquement celui-ci ; sinon null. */
    @Column(length = 500)
    private String formattedAddress;

    /** Latitude (géocodage) precision=10 : nombre total de chiffres (avant + après la virgule) scale=8:nombre de chiffres après la virgule.  */
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    /** Longitude (géocodage) */
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
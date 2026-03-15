package com.example.coopachat.entities;

import com.example.coopachat.enums.CouponStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Promotion produit/catégorie (réduction automatique sur des produits).
 * Une seule promotion peut être active à la fois.
 * Les produits concernés et la réduction par produit sont dans PromotionProduct.
 */
@Entity
@Table(name = "promotions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Le nom de la promotion est obligatoire")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Le statut est obligatoire")
    private CouponStatus status;

    @Column(nullable = false)
    private Boolean isActive = false;

    @Column(nullable = false)
    @NotNull(message = "La date de début est obligatoire")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime startDate;

    @Column(nullable = false)
    @NotNull(message = "La date de fin est obligatoire")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime endDate;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

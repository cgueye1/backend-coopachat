package com.example.coopachat.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity représentant un produit
 */
@Entity
@Table(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String productCode; // Code unique du produit

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Le nom du produit est obligatoire")
    private String name; // Nom du produit

    @Column(columnDefinition = "TEXT")
    private String description; // Description du produit

    @Column(columnDefinition = "TEXT")
    private String image; // Image du produit (URL/fichier)

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "La catégorie est obligatoire")
    private Category category; // Catégorie du produit

    @Column(nullable = false)
    @NotNull(message = "Le prix unitaire est obligatoire")
    @Positive(message = "Le prix doit être positif")
    private BigDecimal price; // Prix unitaire

    @Column(nullable = false)
    @NotNull(message = "Le stock actuel est obligatoire")
    @PositiveOrZero(message = "Le stock ne peut pas être négatif")
    private Integer currentStock; // Stock actuel (quantité disponible)

    @Column(nullable = false)
    @NotNull(message = "Le seuil minimum est obligatoire")
    @PositiveOrZero(message = "Le seuil minimum ne peut pas être négatif")
    private Integer minThreshold; // Seuil minimum de réapprovisionnement

    @Column(nullable = false)
    private Boolean status = false; // Statut actif/inactif (true = actif, false = inactif)

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt; // Date de création

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification
}



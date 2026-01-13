package com.example.coopachat.entities;

import com.example.coopachat.enums.SupplierOrderStatus;
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

import java.time.LocalDateTime;

/**
 * Entity représentant une commande fournisseur
 */
@Entity
@Table(name = "supplier_orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String orderNumber; // Code unique de la commande (ex: "CMD-2025-001")

    @Column(nullable = false)
    @NotBlank(message = "Le nom du fournisseur est obligatoire")
    private String supplierName; // Nom du fournisseur (saisie libre)

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Le produit est obligatoire")
    private Product product; // Produit commandé (un seul produit par commande)

    @Column(nullable = false)
    @NotNull(message = "La quantité commandée est obligatoire")
    @Positive(message = "La quantité commandée doit être positive")
    private Integer quantityOrdered; // Quantité commandée

    @Column(nullable = true)
    @PositiveOrZero(message = "La quantité reçue ne peut pas être négative")
    private Integer quantityReceived; // Quantité reçue (peut être différent de quantityOrdered)

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime orderDate; // Date de commande

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime expectedDate; // Date prévue de livraison

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierOrderStatus status; // Statut de la commande

    @Column(columnDefinition = "TEXT")
    private String notes; // Note optionnelle

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime receivedDate; // Date de réception (quand status = LIVREE)

    @Column(columnDefinition = "TEXT")
    private String deliveryNote; // Bon de livraison (URL/fichier)

    @ManyToOne
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private Users createdBy; // Responsable Logistique qui a créé la commande

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification
}


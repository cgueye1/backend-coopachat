package com.example.coopachat.entities;

import com.example.coopachat.enums.SupplierOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.persistence.Convert;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private String orderNumber; // Code unique de la commande

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    @NotNull(message = "Le fournisseur est obligatoire")
    private Supplier supplier; // Fournisseur référencé (dropdown)

    @OneToMany(mappedBy = "supplierOrder", cascade = CascadeType.ALL, orphanRemoval = true) //la suppression de la commande supprime automatiquement tous ses items.
    private List<SupplierOrderItem> items = new ArrayList<>(); // Liste des produits dans la commande

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime orderDate; // Date de commande

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime expectedDate; // Date prévue de livraison

    @Column(nullable = false, length = 32, columnDefinition = "VARCHAR(32) NOT NULL")
    @Enumerated(EnumType.STRING)
    private SupplierOrderStatus status; // Statut de la commande (valeurs: EN_ATTENTE, EN_COURS, LIVREE, ANNULEE)

    @Column(columnDefinition = "TEXT")
    private String notes; // Note optionnelle

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime receivedDate; // Date de réception (quand status = LIVREE)

    @Column(columnDefinition = "TEXT")
    private String deliveryNote; // Bon de livraison (URL/fichier)

    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private Users createdBy; // Responsable Logistique qui a créé la commande

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification
}


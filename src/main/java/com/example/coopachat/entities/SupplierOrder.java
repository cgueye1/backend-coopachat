package com.example.coopachat.entities;

import com.example.coopachat.enums.SupplierOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
    private String orderNumber; // Code unique de la commande

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier; // Fournisseur

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierOrderStatus status; // Statut de la commande

    @Column(columnDefinition = "TEXT")
    private String notes; // Instructions complémentaires ou pièces jointes

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime orderDate; // Date de commande

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime receivedDate; // Date de réception (quand status = LIVREE)

    @Column(columnDefinition = "TEXT")
    private String deliveryNote; // Bon de livraison (URL/fichier)

    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private Users createdBy; // Responsable logistique qui a créé la commande

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification
}


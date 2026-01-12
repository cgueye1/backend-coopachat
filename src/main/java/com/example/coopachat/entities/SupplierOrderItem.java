package com.example.coopachat.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity représentant un produit dans une commande fournisseur
 */
@Entity
@Table(name = "supplier_order_items")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplier_order_id", nullable = false)
    @NotNull(message = "La commande est obligatoire")
    private SupplierOrder supplierOrder; // Commande fournisseur

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Le produit est obligatoire")
    private Product product; // Produit commandé

    @Column(nullable = false)
    @NotNull(message = "La quantité commandée est obligatoire")
    @Positive(message = "La quantité commandée doit être positive")
    private Integer quantityOrdered; // Quantité commandée


    @Column(columnDefinition = "TEXT")
    private String observations; // Manque, erreur - pour chaque produit
}


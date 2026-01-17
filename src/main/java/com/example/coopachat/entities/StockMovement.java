package com.example.coopachat.entities;

import com.example.coopachat.enums.MovementType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity représentant un mouvement de stock (entrée ou sortie)
 */
@Entity
@Table(name = "stock_movements")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Le produit est obligatoire")
    private Product product; // Produit concerné

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Le type de mouvement est obligatoire")
    private MovementType movementType; // Type de mouvement (ENTRY ou EXIT)

    @Column(nullable = false)
    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    private Integer quantity; // Quantité du mouvement

    @Column(nullable = true)
    private String reference; // Référence (ex: "CMD-0012" pour approvisionnement)

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime movementDate; // Date du mouvement

    @Column(columnDefinition = "TEXT")
    private String notes; // Observations
}




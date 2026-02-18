package com.example.coopachat.entities;

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
 * Frais configurables par l'admin (montant fixe uniquement).
 * Exemples : Frais de livraison, Frais d'emballage, etc.
 */
@Entity
@Table(name = "fees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom du frais (ex: "Frais de livraison", "Frais d'emballage")
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Description (optionnelle)
     */
    @Column(length = 500)
    private String description;


    /**
     * Montant du frais (fixe)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Actif ou non (peut être désactivé temporairement)
     */
    @Column(nullable = false)
    private Boolean isActive = true;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

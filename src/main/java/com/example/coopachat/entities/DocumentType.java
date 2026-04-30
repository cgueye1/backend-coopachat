package com.example.coopachat.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity représentant un type de document prérequis (ex: CNI, RIB, etc.)
 */
@Entity
@Table(name = "document_types")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Le nom du type de document est obligatoire")
    private String name; // Titre du document (ex: "Pièce d'identité")

    @ElementCollection
    @CollectionTable(name = "document_type_synonyms", joinColumns = @JoinColumn(name = "document_type_id"))
    @Column(name = "synonym")
    private Set<String> synonyms = new HashSet<>(); // Liste des synonymes (CNI, Passeport, etc.)

    @Column(nullable = false)
    private Boolean hasExpiryDate = false; // Ce document a-t-il une date d'expiration ?

    @Column(nullable = false)
    private Boolean isIdentityVerification = false; // Ce document sert-il à vérifier l'identité ?

    @Column(nullable = false)
    private Boolean isActive = true; // Statut actif/inactif

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

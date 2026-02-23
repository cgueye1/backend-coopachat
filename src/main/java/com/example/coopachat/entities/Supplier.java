package com.example.coopachat.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity représentant un fournisseur
 */
@Entity
@Table(name = "suppliers")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Le nom du fournisseur est obligatoire")
    private String name; // Nom du fournisseur

    @Column(nullable = false, unique = true)
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email; // Email du fournisseur

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,25}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres")
    private String phone; // Numéro de téléphone

    @Column(nullable = false)
    @NotBlank(message = "L'adresse est obligatoire")
    private String address; // Adresse du fournisseur

    @Column(nullable = false)
    private Boolean isActive = true; // Statut actif/inactif (défaut: true)
}





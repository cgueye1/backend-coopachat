package com.example.coopachat.entities;

import com.example.coopachat.enums.SupplierType;
import java.util.Set;
import java.util.HashSet;

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
    private String name; // Nom du fournisseur (nom_fournisseur)

    @Enumerated(EnumType.STRING)
    @Column(name = "type_fournisseur")
    private SupplierType type; // Type de fournisseur (type_fournisseur)

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "supplier_categories",
        joinColumns = @JoinColumn(name = "supplier_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>(); // Liste des catégories (secteur_activite)

    @Column(columnDefinition = "TEXT")
    private String description; // Description

    @Column(nullable = false)
    @NotBlank(message = "L'adresse est obligatoire")
    private String address; // Adresse (adresse)

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,25}$",
            message = "Le numéro de téléphone doit être valide")
    private String phone; // Téléphone (telephone)

    @Column(nullable = false, unique = true)
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email; // Email (email)

    @Column
    private String contactName; // Nom du contact (nom_contact)

    @Column(unique = true)
    private String ninea; // NINEA / Registre du commerce (numero_registre_commerce)

    @Column
    private String deliveryTime; // Délai de livraison (delai_livraison)

    @Column(nullable = false)
    private Boolean isActive = true; // Statut actif/inactif (statut)
}





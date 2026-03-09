package com.example.coopachat.entities;

import com.example.coopachat.enums.CompanyStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "companies")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class  Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String companyCode; // Code unique de l'entreprise

    @Column(nullable = false)
    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String name; // Nom de l'entreprise

    @ManyToOne
    @JoinColumn(name = "company_sector_id")
    private CompanySector sector; // Secteur d'activité (référentiel admin)

    @Column(nullable = false)
    @NotBlank(message = "La localisation est obligatoire")
    private String location; // Localisation (adresse ou région)

    @Column(nullable = false)
    @NotBlank(message = "Le nom du contact est obligatoire")
    private String contactName; // Nom du contact

    @Email(message = "L'email du contact doit être valide")
    @Column(nullable = true)
    private String contactEmail; // Email du contact

    @Column(nullable = false)
    @NotBlank(message = "Le téléphone du contact est obligatoire")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,25}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres")
    private String contactPhone; // Téléphone du contact

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status; // Statut de prospection

    @Column(columnDefinition = "TEXT")
    private String note; // Commentaire ou note

    /** Nom du fichier logo (ex. uuid.png), servi via GET /api/files/{filename}. Null si non renseigné. */
    @Column(name = "logo", length = 255)
    private String logo;

    @Column(nullable = false)
    private Boolean isActive = true; // Entreprise activée ou non

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt; // Date de création

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification

    // Relations
    @ManyToOne
    @JoinColumn(name = "commercial_id", nullable = false)
    private Users commercial; // Commercial qui gère l'entreprise

   @OneToMany (mappedBy = "company")
   private List<Employee> employees; // Liste des salariés de l'entreprise
}
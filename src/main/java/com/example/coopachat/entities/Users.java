package com.example.coopachat.entities;

import com.example.coopachat.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Référence utilisateur unique (ex. US-A1B2C3D4), générée aléatoirement à la création. */
    @Column(name = "ref_user", unique = true, length = 20)
    private String refUser;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;


    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,25}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres")
    @Column(unique = true)
    private String phone;

    @Column(unique = true, nullable = false)
    @Email(message = "L'email doit être valide")
    private String email;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = true)
    private String companyCommercial;  //Entreprise liée (commercial)


    @Column(nullable = false)
    private Boolean isActive = false;

    /**
     * Indique que le compte a été suspendu manuellement par un administrateur.
     * Permet de distinguer un compte jamais activé (isActive=false, disabledByAdmin=false)
     * d'un compte suspendu (isActive=false, disabledByAdmin=true).
     */
    @Column(nullable = false)
    private Boolean disabledByAdmin = false;

    /** Nom du fichier photo de profil (ex. uuid.jpg), servi via GET /api/files/{filename}. Null si non renseigné. */
    @Column(name = "profile_photo_url", length = 255)
    private String profilePhotoUrl;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

}
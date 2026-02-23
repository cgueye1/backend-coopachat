package com.example.coopachat.dtos.user;

import com.example.coopachat.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO unique pour la création et la modification d'un utilisateur par l'admin.
 * Création : firstName, lastName, email, phoneNumber, role obligatoires.
 * Modification : seuls les champs envoyés (non null) sont mis à jour.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveUserDTO {

    private String firstName;
    private String lastName;

    @Email(message = "L'email doit être valide")
    private String email;

    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,15}$", message = "Le numéro de téléphone doit contenir entre 8 et 15 caractères")
    private String phoneNumber;

    private UserRole role;

    /** Optionnel : entreprise liée (nom), utilisé lorsque le rôle est Commercial. */
    private String companyCommercial;
}

package com.example.coopachat.dtos;

import com.example.coopachat.enums.CompanySector;
import com.example.coopachat.enums.CompanyStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la modification d'une entreprise
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCompanyDTO {

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String name; // Nom de l'entreprise

    private CompanySector sector; // Secteur d'activité (optionnel)

    @NotBlank(message = "La localisation est obligatoire")
    private String location; // Localisation (adresse ou région)

    @NotBlank(message = "Le nom du contact est obligatoire")
    private String contactName; // Nom du contact

    @Email(message = "L'email du contact doit être valide")
    private String contactEmail; // Email du contact (optionnel)

    @NotBlank(message = "Le téléphone du contact est obligatoire")
    @Pattern(regexp = "^[0-9]{8,15}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres uniquement")
    private String contactPhone; // Téléphone du contact

    @NotNull(message = "Le statut de prospection est obligatoire")
    private CompanyStatus status; // Statut de prospection

    private String note; // Commentaire ou note (optionnel)
}



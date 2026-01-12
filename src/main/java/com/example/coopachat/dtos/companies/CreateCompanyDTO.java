
package com.example.coopachat.dtos.companies;

import com.example.coopachat.enums.CompanySector;
import com.example.coopachat.enums.CompanyStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la création d'une entreprise par un commercial
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanyDTO {

    @JsonProperty (access = JsonProperty.Access.READ_ONLY)
    private Long id; // ID de l'entreprise (généré après création)

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String companyCode; // Code unique de l'entreprise (généré automatiquement)

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String name; // Nom de l'entreprise

    private CompanySector sector; // Secteur d'activité (optionnel)

    @NotBlank(message = "La localisation est obligatoire")
    private String location; // Localisation (adresse ou région)

    @NotBlank(message = "Le nom du contact est obligatoire")
    private String contactName; // Nom du contact

    @Email(message = "L'email du contact doit être valide")
    private String contactEmail; // Email du contact

    @NotBlank(message = "Le téléphone du contact est obligatoire")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,25}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres")
    private String contactPhone; // Téléphone du contact

    @NotNull(message = "Le statut de prospection est obligatoire")
    private CompanyStatus status; // Statut de prospection

    private String note; // Commentaire ou note (optionnel)
}
package com.example.coopachat.dtos.companies;

import com.example.coopachat.enums.CompanyStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la modification partielle d'une entreprise.
 * Tous les champs sont optionnels : seuls ceux envoyés sont mis à jour.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCompanyDTO {

    private String name;

    private Long sectorId;

    private String location;

    private String contactName;

    @Email(message = "L'email du contact doit être valide")
    private String contactEmail;

    @Pattern(regexp = "^[0-9]{8,15}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres uniquement")
    private String contactPhone;

    private CompanyStatus status; // ex. PARTNER_SIGNED pour « Partenaire signé »

    private String note;
}



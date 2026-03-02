package com.example.coopachat.dtos.companies;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.coopachat.enums.CompanySector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les détails d'une entreprise
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyDetailsDTO {

    private Long id;
    private String name;
    private String location;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;
    
    private String status; // Libellé du statut de prospection (ex. Partenaire signé, En attente)
    private String companyCode; // Code unique de l'entreprise
    private CompanySector sector; // Secteur d'activité
    private String note; // Commentaires ou notes
    private String logo; // Nom du fichier logo (pour affichage via /api/files/{filename})
    private Boolean isActive; // Entreprise activée (toggle)

    /** Nombre de salariés (uniquement pour entreprise partenaire, sinon null). */
    private Long employeeCount;
    /** Nombre de commandes passées par les salariés de cette entreprise (uniquement partenaire, sinon null). */
    private Long orderCount;
}



package com.example.coopachat.dtos.companies;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

// DTO pour la liste des entreprises

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyListItemDTO {

    private Long id ;
    private String name;
    private String sector;
    private String sectorLabel; // Libellé du secteur (nom) pour le frontend

    private String location;
    private String contactName;
    private String contactPhone;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;

    private String status; // Libellé du statut de prospection (ex. Partenaire signé)
    private String logo; // Nom du fichier logo pour affichage
    private Boolean isActive; // true = Actif, false = Inactif (activation manuelle)

}

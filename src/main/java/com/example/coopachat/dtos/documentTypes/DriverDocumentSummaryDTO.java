package com.example.coopachat.dtos.documentTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDocumentSummaryDTO {
    // Infos livreur
    private Long driverId;
    private String driverRef;
    private String firstName;
    private String lastName;
    private String profilePhotoUrl;

    // Statut global calculé
    private String globalStatus;      // EN_ATTENTE | INCOMPLET | VALIDE | REJETE
    private String globalStatusLabel; // "En attente" | "Incomplet" | "Validé" | "Rejeté"
    
    // Pour afficher par exemple "2/3 documents soumis"
    private int submittedCount;
    private int requiredCount;

    // Dernière date de soumission d'un document par ce livreur
    private LocalDateTime lastSubmissionDate;
}

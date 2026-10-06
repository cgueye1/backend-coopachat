package com.example.coopachat.dtos.documentTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverDocumentListItemDTO {
    private Long documentTypeId;
    private String name;
    private Boolean hasExpiryDate;
    private Boolean isIdentityVerification;
    private Boolean isMandatory;
    
    // Informations de soumission (null si pas encore soumis)
    private String status; // "NON_SOUMIS", "EN_ATTENTE", "VALIDE", "REJETE"
    private String statusLabel; // Pour l'affichage direct (ex: "Non soumis", "En vérification")
    private String fileUrl;
    private String fileVersoUrl;
    private String rejectionReason;
    
    private String submittedAt;
    private String issueDate;
    private String expirationDate;
}

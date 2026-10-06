package com.example.coopachat.dtos.documentTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverDocumentDetailDTO {
    private Long documentTypeId;
    private String name;
    
    // Dates
    private LocalDateTime submittedAt;
    private LocalDate issueDate;
    private LocalDate expirationDate;
    
    // Status
    private String status;
    private String statusLabel;
    private String rejectionReason;
    
    // Images
    private String fileUrl;
    private String fileVersoUrl;
    
    // Informations sur le fournisseur (Livreur)
    private String providerRef;
    private String providerFullName;
}

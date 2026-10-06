package com.example.coopachat.dtos.documentTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmitDriverDocumentDTO {
    private Long documentTypeId;
    private String fileUrl; // URL du fichier uploadé (recto)
    private String fileVersoUrl; // URL du fichier uploadé (verso)
    private LocalDate issueDate;
    private LocalDate expirationDate;
}

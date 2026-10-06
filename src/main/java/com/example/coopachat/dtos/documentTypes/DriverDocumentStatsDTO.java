package com.example.coopachat.dtos.documentTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverDocumentStatsDTO {
    private long totalDriversWithDocuments;
    private long pendingDocuments;
    private long validatedDocuments;
    private long rejectedDocuments;
    private long totalDrivers;
}
